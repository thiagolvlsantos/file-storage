package com.thiagolvlsantos.git.commons.file;

import java.io.File;

import lombok.experimental.UtilityClass;

//TODO: create a git commons
@UtilityClass
public class FileUtils {

	public static boolean mkdirs(File dir) {
		return dir.mkdirs();
	}

	public static boolean delete(File f) {
		boolean ok = true;
		if (f.isDirectory()) {
			for (File c : f.listFiles()) {
				ok = ok & delete(c);
			}
		}
		return ok & f.delete();
	}
}
