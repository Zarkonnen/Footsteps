/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.zarkonnen.mamo15;

import com.zarkonnen.catengine.Condition;
import com.zarkonnen.catengine.SlickEngine;
import java.io.File;

/**
 *
 * @author zar
 */
public class Main {

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		/*if (System.getProperty("os.name", "NONE").toLowerCase().contains("win")) {
			System.setProperty("java.library.path", new File("").getAbsolutePath() + "\\lib\\native");
			System.setProperty("org.lwjgl.librarypath", new File("").getAbsolutePath() + "\\lib\\native");
		} else {
			System.setProperty("java.library.path", new File("").getAbsolutePath() + "/lib/native");
			System.setProperty("org.lwjgl.librarypath", new File("").getAbsolutePath() + "/lib/native");
		}*/
		
		SlickEngine se = new SlickEngine("Footsteps", "/com/zarkonnen/mamo15/images/", "/com/zarkonnen/mamo15/sounds/", 60);
		se.setup(new MamoGame());
		se.runUntil(Condition.ALWAYS);
	}
	
}
