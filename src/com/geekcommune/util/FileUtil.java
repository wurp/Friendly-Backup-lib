package com.geekcommune.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.geekcommune.friendlybackup.logging.UserLog;

public class FileUtil {
    private static final Logger log = Logger.getLogger(FileUtil.class);

	public byte[] getFileContents(File f) throws IOException {
	    if( f.length() > Integer.MAX_VALUE ) {
	    	throw new IOException("Not reading full contents of > 2 gig file into memory");
	    }
	    
		byte[] retval = new byte[(int)f.length()];
		
		FileInputStream fis = new FileInputStream(f);
        BufferedInputStream bis = new BufferedInputStream(fis);
		try {
		    int bytesRead;
		    int total = 0;

		    while( (bytesRead = bis.read(retval, total, retval.length - total)) != -1 && total != retval.length ) {
		        total += bytesRead;
		    }
		} finally {
		    bis.close();
		}
		
		return retval;
	}
    
    /**
     * Populates result with all files in the tree rooted at 'root'.  Depth first recursion.
     * 
     * @param result
     * @param root
     * @param ff
     */
    public void listTree(final List<File> result, File root) {
        listTree(result, root, new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });
    }
    
    /**
     * Populates result with all files in the tree rooted at 'root' which match ff.  Depth first recursion.
     * 
     * @param result
     * @param root
     * @param ff
     */
    public void listTree(final List<File> result, File root, final FileFilter ff) {
        File[] files = root.listFiles(new FileFilter() {
            
            public boolean accept(File pathname) {
                if( pathname.isDirectory() ) {
                    listTree(result, pathname, ff);
                }
                
                return ff.accept(pathname);
            }
        });
        
        if( files != null ) {
            for(File f : files) {
                result.add(f);
            }
        }
    }

	public static FileUtil instance() {
		return new FileUtil();
	}

    public void createPathAndWriteFile(File file, byte[] t) {
        file.getParentFile().mkdirs();
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(t);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
            UserLog.instance().logError("No directory in which to create file " + file, e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            UserLog.instance().logError("Failed to create file " + file + ", " + e.getMessage(), e);
        } finally {
        	close(out, "Failed while closing file " + file);
        } //end try/finally
    }

	public void close(BufferedOutputStream out, String message) {
		close(out, message, log);
	}

	public void close(Closeable stream, String message, Logger logParam) {
		if( stream != null ) {
			try {
				stream.close();
			} catch (IOException e) {
				logParam.error(
						message + ": " + e.getMessage(),
						e);
			}
		}
	}
}
