package org.sleeksnap.filter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.sleeksnap.util.Util;

/**
 * An Experimental upload filter for compressing images through pngcrush or pngout
 * Warning: May make uploads slower than they already are! Crushing big images takes time.
 * 
 * @author Nikki
 *
 */
public class PNGCompressionFilter implements UploadFilter<BufferedImage> {
	
	/**
	 * Logger object
	 */
	private static final Logger logger = Logger.getLogger(PNGCompressionFilter.class.getName());

	/**
	 * Location of the pngcrush program
	 * pngcrush is faster than pngout.
	 * Download: http://pmt.sourceforge.net/pngcrush/
	 */
	private static File pngCrush = new File(Util.getWorkingDirectory(), "tools/pngcrush.exe");
	
	/**
	 * Location of the pngout program
	 * pngout is slower than pngcrush, but has higher compression ratios.
	 * Download: http://advsys.net/ken/util/pngout.exe
	 */
	private static File pngOut = new File(Util.getWorkingDirectory(), "tools/pngout.exe");
	
	@Override
	public BufferedImage filter(BufferedImage object) {
		if(pngOut.exists() || pngCrush.exists()) {
			try {
				File input = new File(Util.getWorkingDirectory(), "sleeksnap_original.png");
				File output = new File(Util.getWorkingDirectory(), "sleeksnap_compressed.png");
				
				ImageIO.write(object, "png", input);
				
				String[] opts = new String[3];
				opts[0] = strPad((pngOut.exists() ? pngOut : pngCrush).getAbsolutePath(), '"');
				opts[1] = strPad(input.getAbsolutePath(), '"');
				opts[2] = strPad(output.getAbsolutePath(), '"');
				
				logger.info("Compressing image with "+(pngOut.exists() ? "pngout" : "pngcrush") +"...");
				
				Process p = Runtime.getRuntime().exec(opts);
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				while(reader.readLine() != null) {
					//Nothing.
				}
				
				logger.info("Compressed image, original size: "+input.length()+", compressed size: "+output.length());
				
				try {
					//Finally, read the new file.
					return ImageIO.read(output);
				} finally {
					input.delete();
					output.delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return object;
	}
	
	@Override
	public Class<?> getType() {
		return BufferedImage.class;
	}

	private static String strPad(String string, char pad) {
		return pad + string + pad;
	}
}
