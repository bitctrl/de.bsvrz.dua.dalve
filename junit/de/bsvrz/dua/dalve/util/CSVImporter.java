/** 
 * Segment 4 Datenübernahme und Aufbereitung (DUA)
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
 
package de.bsvrz.dua.dalve.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

import sys.funclib.debug.Debug;

/**
 * Allgemeine Klasse zum Lesen aus CSV-Dateien
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class CSVImporter{

	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();

	/**
	 * CSV-Datei
	 */
	private File csvDatei = null;

	/**
	 * der Reader für die CSV-Datei
	 */
	private BufferedReader leser = null;


	/**
	 * Standardkonstruktor Nr.1
	 * 
	 * @param csvDatei CSV-Datei
	 * @throws Exception wenn die Datei nicht geöffnet werden kann
	 */
	public CSVImporter(final File csvDatei)
	throws Exception{
		this.csvDatei = csvDatei;
		this.leser = new BufferedReader(new FileReader(csvDatei));
	}

	
	/**
	 * Standardkonstruktor Nr.2
	 * 
	 * @param csvDateiName Name der CSV-Datei (mit oder ohne Suffix)
	 * @throws Exception wenn die Datei nicht geöffnet werden kann
	 */
	public CSVImporter(final String csvDateiName)
	throws Exception{
		String name = csvDateiName;
		if(!name.toLowerCase().endsWith(".csv")){ //$NON-NLS-1$
			name += ".csv"; //$NON-NLS-1$
		}
		this.csvDatei = new File(name);
		this.leser = new BufferedReader(new FileReader(csvDatei));
	}

	
	/**
	 * Gibt alle Spalten einer Zeile der Tabelle als
	 * String-Array zurück
	 * 
	 * @return ein String-Array mit den Spalten einer Zeile
	 * oder <code>null</code>, wenn das Dateiende erreicht ist
	 */
	public final String[] getNaechsteZeile(){
		String[] result = null;

		try{
			String red = leser.readLine();

			if(red != null){
				result = red.split(";"); //$NON-NLS-1$
			}
		}catch(IOException ex){
			LOGGER.error("Fehler beim Lesen aus " + this, ex); //$NON-NLS-1$
		}

		return result;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String s = csvDatei.toString();
		
		try {
			s = csvDatei.getCanonicalPath();
		} catch (IOException ex) {
			LOGGER.error(Konstante.LEERSTRING, ex);
		}
		
		return s;
	}
	
	
	/**
	 * Setzt den Dateizeiger wieder auf Anfang
	 */
	public final void reset(){
		try {
			this.leser.close();
			this.leser = new BufferedReader(new FileReader(this.csvDatei));
		} catch (IOException ex) {
			LOGGER.error(Konstante.LEERSTRING, ex);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finalize()
	throws Throwable {
		if(this.leser != null){
			this.leser.close();
		}
	}

}