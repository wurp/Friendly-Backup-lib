package com.geekcommune.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.communication.message.HasResponseHandler;
import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.util.Pair;
import com.geekcommune.util.UnaryContinuation;

public abstract class MessageUtil {
    public static final Logger log = Logger.getLogger(MessageUtil.class);

	public static final int MAX_TRIES = 5;

	public static final int NUM_THREADS = 10;

	public Executor sendExecutor;

	public ConcurrentHashMap<RemoteNodeHandle, AtomicInteger> destinationFailures = new ConcurrentHashMap<RemoteNodeHandle, AtomicInteger>();

	public ConcurrentHashMap<RemoteNodeHandle, AtomicInteger> destinationSuccesses = new ConcurrentHashMap<RemoteNodeHandle, AtomicInteger>();

	/**
	 * Map from transaction id to the continuation for handling the data requested in that xaction
	 */
	public ConcurrentHashMap<Integer, UnaryContinuation<Message>> responseHandlers = new ConcurrentHashMap<Integer, UnaryContinuation<Message>>();

	private ConcurrentHashMap<Pair<InetAddress, Integer>, Socket> socketMap = new ConcurrentHashMap<Pair<InetAddress,Integer>, Socket>();

	private ConcurrentHashMap<Socket, Lock> socketLockMap = new ConcurrentHashMap<Socket, Lock>();

	private Thread listenThread;

	private ThreadPoolExecutor listenExecutor;

	protected List<MessageHandler> messageHandlers = new ArrayList<MessageHandler>();

	public MessageUtil() {
        LinkedBlockingQueue<Runnable> sendWorkQueue = new LinkedBlockingQueue<Runnable>();
        sendExecutor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 1000, TimeUnit.MILLISECONDS, sendWorkQueue);
	}
	
	public void queueMessage(final Message msg) {
        msg.setState(Message.State.NeedsProcessing);
	    if( msg.getState() == Message.State.NeedsProcessing ) {
	        msg.setState(Message.State.Queued);
	        sendExecutor.execute(
	                new Runnable() {
	                    public void run() {
	                        msg.setState(Message.State.Processing);
	
	                        if( msg instanceof HasResponseHandler ) {
	                        	HasResponseHandler rdm = (HasResponseHandler) msg;
	                            responseHandlers.put(
	                                    rdm.getTransactionID(),
	                                    rdm.getResponseHandler());
	                        }
	                        send(msg);
	                    }
	                });
	    }
	}

	protected void send(Message msg) {
	    //TODO handle proxying somehow someday :-)
	    Message.State state = Message.State.Error;
	    
	    Socket socket = null;
	    log.debug("sending "+ msg.getTransactionID());
	    try {
	        log.debug("Attempting to talk to " + msg.getDestination().getAddress() + ":" + msg.getDestination().getPort());
	        socket = acquireSocket(msg.getDestination().getAddress(), msg.getDestination().getPort());
	
	        justSend(msg, socket);
	        state = Message.State.Finished;
	        
	        destinationSuccesses.putIfAbsent(msg.getDestination(), new AtomicInteger(0));
	        destinationSuccesses.get(msg.getDestination()).incrementAndGet();
	    } catch (IOException e) {
	        removeSocket(socket);
	        
	        destinationFailures.putIfAbsent(msg.getDestination(), new AtomicInteger(0));
	        destinationFailures.get(msg.getDestination()).incrementAndGet();
	        
	        msg.setNumberOfTries(msg.getNumberOfTries() + 1);
	        if( msg.getNumberOfTries() > MAX_TRIES ) {
	            UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName(), e);
	            log.error("Not retrying, exceeded max tries: " + e.getMessage(), e);
	        } else {
	            UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName() + ", will retry", e);
	            log.error("Failed to send message to " + msg.getDestination().getName() + ", will retry: " + e.getMessage(), e);
	            
	            state = Message.State.NeedsProcessing;
	            queueMessage(msg);
	        }
	    } catch (FriendlyBackupException e) {
	        UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName(), e);
	    } finally {
	        msg.setState(state);
	        
	        if( socket != null ) {
	            releaseSocket(socket);
	            log.debug("Finished talking");
	        }
	    }
	    log.debug("sent "+ msg.getTransactionID());
	}

	private void justSend(Message msg, Socket socket) throws IOException,
			FriendlyBackupException {
			    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			    msg.write(dos);
			    dos.flush();
			
			    //block until message is processed & response sent
			    socket.getInputStream().read();
			}

	public void releaseSocket(Socket socket) {
	    Lock lock = socketLockMap.get(socket);
	    
	    if( lock != null ) {
	        lock.unlock();
	    }
	}

	public void removeSocket(Socket socket) {
	    if( socket != null ) {
	        Lock lock = socketLockMap.get(socket);
	        
	        if( lock != null ) {
	            lock.unlock();
	            socketLockMap.remove(socket);
	        }
	        
	        Pair<InetAddress, Integer> key = new Pair<InetAddress, Integer>(socket.getInetAddress(), socket.getPort());
	        socketMap.remove(key);
	    }
	}

	public Socket acquireSocket(InetAddress address, int port) throws IOException {
	    Pair<InetAddress, Integer> key = new Pair<InetAddress, Integer>(address, port);
	    Socket socket = socketMap.putIfAbsent(key, new Socket());
	    if( socket == null ) {
	        socket = socketMap.get(key);
	    }
	
	    //TODO not entirely thread safe
	    socketLockMap.putIfAbsent(socket, new ReentrantLock());
	    Lock lock = socketLockMap.get(socket);
	    lock.lock();
	    
	    if( !socket.isConnected() ) {
	        socket.connect(new InetSocketAddress(address, port));
	    }
	
	    return socket;
	}

	public void startListenThread() {
	    if( listenThread != null ) {
	        throw new RuntimeException("Listen thread already started");
	    }
	
	    LinkedBlockingQueue<Runnable> listenWorkQueue = new LinkedBlockingQueue<Runnable>();
	    listenExecutor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 1000, TimeUnit.MILLISECONDS, listenWorkQueue);
	
	    listenThread = new Thread(new Runnable() {
	        public void run() {
	            try {
	                ServerSocket serversocket = null;
	                serversocket = new ServerSocket(getLocalPort());
	                log.debug("server socket listening on " + getLocalPort());
	                do {
	                    try {
	                        Socket socket = serversocket.accept();
	                        log.debug("Server socket open");
	                        listenExecutor.execute(makeHandleAllMessagesOnSocketRunnable(socket));
	                    } catch(Exception e) {
	                        log.error(e.getMessage(), e);
	                    }
	                } while (true);
	            } catch (Exception e) {
	                e.printStackTrace();
	                log.error("Couldn't start listening for unsolicited messages: " + e.getMessage(), e);
	            }
	        }
	    });
	
	    listenThread.start();
	}

	private Runnable makeHandleAllMessagesOnSocketRunnable(final Socket socket) {
	    return new Runnable() {
	        public void run() {
	            try {
	                while(socket.isConnected() && !socket.isInputShutdown()) {
	                    DataInputStream dis = new DataInputStream(socket.getInputStream());
	                    final Message msg = AbstractMessage.parseMessage(dis);
	                    final InetAddress address = socket.getInetAddress();
	                    msg.setState(Message.State.NeedsProcessing);
	                    makeProcessMessageRunnable(msg, address).run();
	                    socket.getOutputStream().write(1);
	                    socket.getOutputStream().flush();
	                }
	            } catch (IOException e) {
	                log.error("Error talking to " + socket + ", " + e.getMessage(), e);
	            } catch (FriendlyBackupException e) {
	                log.error("Error talking to " + socket + ", " + e.getMessage(), e);
	            } finally {
	                try {
	                    log.debug("Server socket finished");
	                    socket.close();
	                } catch( Exception e ) {
	                    log.error("Error closing socket to " + socket + ", " + e.getMessage(), e);
	                }
	            }
	        }
	    };
	}

	private Runnable makeProcessMessageRunnable(final Message msg, final InetAddress address) {
	    return new Runnable() {
	        public void run() {
	            try {
	                processMessage(msg, address);
	            } catch (SQLException e) {
	                log.error("Error talking processing " + msg + ": " + e.getMessage(), e);
	            } catch (FriendlyBackupException e) {
	                log.error("Error talking processing " + msg + ": " + e.getMessage(), e);
	            }
	        }
	    };
	}


	/**
	 * Each message handler will get every message until one of them says it's handled.
	 * @see MessageHandler
	 * @param messageHandler
	 */
	public void addMessageHandler(MessageHandler messageHandler) {
		messageHandlers.add(messageHandler);
	}

	public void processMessage(Message msg, InetAddress inetAddress)
			throws SQLException, FriendlyBackupException {
			    log.debug("processing " + msg.getTransactionID());
			    //TODO reject message if we've already processed its transaction id
			    msg.setState(Message.State.Processing);
			
			    UnaryContinuation<Message> responseHandler =
			        responseHandlers.get(msg.getTransactionID());
			
			    if( responseHandler != null ) {
			        responseHandler.run(msg);
			    }
			
			    boolean messageHandled = false;
			    for(MessageHandler mh : messageHandlers ) {
			    	if( mh.handleMessage(msg, inetAddress, responseHandler != null) ) {
			    		messageHandled = true;
			    		break;
			    	}
			    }
			
			    if( !messageHandled ) {
			        msg.setState(Message.State.Error);
			        log.error("Unexpected message type; message: " + msg + " from inetAddress " + inetAddress);
			    }
			
			    log.debug("processed " + msg.getTransactionID());
			}

	protected abstract int getLocalPort();

}
