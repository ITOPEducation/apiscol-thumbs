package fr.ac_versailles.crdp.apiscol.thumbs.fileSystemAccess;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.core.header.FormDataContentDisposition;

import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class ResourceDirectoryInterface {

	private static final int STEP = 25;
	public static final int MAX_THUMB_DIMENSION = 128;
	private static boolean initialized = false;
	private static String fileRepoPath;
	private static Logger logger;

	public static boolean isInitialized() {
		return initialized;
	}

	public static void initialize(String path) {
		fileRepoPath = path;
		ignoreSSl();
		initializeLogger();
	}

	private static void ignoreSSl() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(
					java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (GeneralSecurityException e) {
		}
	}

	public static String getFilePath(String thumbId) {
		String root = FileUtils.getFilePathHierarchy(fileRepoPath, thumbId);
		return new StringBuilder().append(root).append(".png").toString();
	}

	public static boolean storeAndResizeThumb(String thumbId, String imageUrl) {
		URL url;
		try {
			url = new URL(imageUrl);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return false;
		}
		BufferedImage image;
		try {
			image = ImageIO.read(url);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return resizeThumb(image, thumbId);
	}

	private static boolean resizeThumb(BufferedImage image, String thumbId) {
		int width, height;
		if (image.getWidth() > MAX_THUMB_DIMENSION
				|| image.getHeight() > MAX_THUMB_DIMENSION) {
			if (image.getWidth() > image.getHeight()) {
				width = MAX_THUMB_DIMENSION;
				height = -1;
			} else {
				width = -1;
				height = MAX_THUMB_DIMENSION;
			}
			Image thumbnail = image.getScaledInstance(width, height,
					Image.SCALE_SMOOTH);
			image = new BufferedImage(thumbnail.getWidth(null),
					thumbnail.getHeight(null), BufferedImage.TYPE_INT_ARGB);
			image.getGraphics().drawImage(thumbnail, 0, 0, null);
		}
		return writeToDisk(image, thumbId);
	}

	private static boolean writeToDisk(BufferedImage image, String thumbId) {
		File outputFile = new File(getFilePath(thumbId));
		outputFile.mkdirs();

		try {
			ImageIO.write(image, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean eraseThumb(String thumbId) {
		File thumbFile = new File(getFilePath(thumbId));
		File parent = thumbFile.getParentFile();
		File grandParent = parent.getParentFile();
		File grandGrandParent = grandParent.getParentFile();
		boolean success = true;
		if (thumbFile != null && thumbFile.exists()) {
			success &= thumbFile.delete();
			if (success && parent.list().length == 0) {
				success &= FileUtils.deleteDir(parent);
				if (success && grandParent.list().length == 0) {
					success &= FileUtils.deleteDir(grandParent);
					if (success && grandGrandParent.list().length == 0) {
						success &= FileUtils.deleteDir(grandGrandParent);
					}
				}
			}
			return success;
		} else {
			logger.warn(String
					.format("The file %s to be deleted is null or does not exist for thumb %s",
							thumbFile.getAbsoluteFile(), thumbId));
			return false;
		}
	}

	public static boolean storeAndResizeCustomThumb(String thumbId,
			InputStream uploadedInputStream,
			FormDataContentDisposition fileDetail) {
		BufferedImage image;
		try {
			image = ImageIO.read(uploadedInputStream);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return resizeThumb(image, thumbId);

	}

	public static String getThumbEtag(String thumbId) {
		File thumbFile = new File(getFilePath(thumbId));
		if (thumbFile.exists())
			return String.valueOf(thumbFile.lastModified());
		return "0";
	}

	public static String getFileName(String thumbId) {
		File thumbFile = new File(getFilePath(thumbId));
		if (thumbFile.exists()) {
			return new StringBuilder().append(thumbId).append(".png")
					.toString();
		}
		return StringUtils.EMPTY;
	}

	public static boolean assignRandomThumb(String thumbId) {
		BufferedImage image = createOneTile(new Color(
				(int) (Math.random() * 255), (int) (Math.random() * 255),
				(int) (Math.random() * 255)));
		return writeToDisk(image, thumbId);
	}

	private static final int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;

	private static BufferedImage createOneTile(final Color c) {
		final Random r = new Random();
		final BufferedImage res = new BufferedImage(MAX_THUMB_DIMENSION,
				MAX_THUMB_DIMENSION, IMAGE_TYPE);
		int step = STEP;
		for (int x = 0; x < res.getWidth(); x += step) {
			for (int y = 0; y < res.getHeight(); y += step) {
				int col = c.getRGB() - r.nextInt(150);
				for (int i = 0; i < step; i++) {
					if (x + i >= res.getHeight())
						continue;
					for (int j = 0; j < step; j++) {
						if (y + j >= res.getHeight())
							continue;
						res.setRGB(x + i, y + j, col);
					}
				}

			}
		}
		return res;
	}

	private static void initializeLogger() {
		if (logger == null)
			logger = LogUtility.createLogger(ResourceDirectoryInterface.class
					.getCanonicalName());

	}

}
