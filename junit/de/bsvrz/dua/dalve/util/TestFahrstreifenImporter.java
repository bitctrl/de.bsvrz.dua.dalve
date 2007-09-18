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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;


/**
 * Liest die Ausgangsdaten für die Prüfung der Datenaufbereitung LVE ein
 * 
 * @author BitCtrl Systems GmbH, Görlitz
 *
 */
public class TestFahrstreifenImporter
extends CSVImporter{
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;
	
	/**
	 * Hält aktuelle Daten des FS 1-3
	 */
	protected String ZEILE[];
	
	/**
	 * T
	 */
	protected static long INTERVALL = Konstante.MINUTE_IN_MS;
	

	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteier-Verbindung
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public TestFahrstreifenImporter(final ClientDavInterface dav, 
								    final String csvQuelle)
	throws Exception{
		super(csvQuelle);
		if(DAV == null){
			DAV = dav;
		}
		
		/**
		 * Tabellenkopf überspringen
		 */
		this.getNaechsteZeile();
	}
	
	
	/**
	 * Setzt Datenintervall
	 * 
	 * @param t Datenintervall
	 */
	public static final void setT(final long t){
		INTERVALL = t;
	}
	
	/**
	 * Importiert die nächste Zeile aus der CSV-Datei
	 *
	 */
	public final void importNaechsteZeile() {
		ZEILE = this.getNaechsteZeile();
	}
	
	/**
	 * Bildet einen Eingabe-Datensatz aus den Daten der aktuellen CSV-Zeile
	 * 
	 * @param FS Fahrstreifen (1-3)
	 * @return ein Datensatz der übergebenen Attributgruppe mit den Daten der nächsten Zeile
	 * oder <code>null</code>, wenn der Dateizeiger am Ende ist
	 */	
	public final Data getDatensatz(final int FS){	
		Data datensatz = DAV.createData(DAV.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD));

		int fsMulti = FS-1;
		
		if(datensatz != null){
			
			if(ZEILE != null){
				try{
					int qKfz = Integer.parseInt(ZEILE[0+(fsMulti*2)]);
					String qKfzStatus = ZEILE[1+(fsMulti*2)];
					int qPkw = Integer.parseInt(ZEILE[6+(fsMulti*2)]);
					String qPkwStatus = ZEILE[7+(fsMulti*2)];
					int qLkw = Integer.parseInt(ZEILE[12+(fsMulti*2)]);
					String qLkwStatus = ZEILE[13+(fsMulti*2)];
					int vKfz = Integer.parseInt(ZEILE[18+(fsMulti*2)]);
					String vKfzStatus = ZEILE[19+(fsMulti*2)];
					int vPkw = Integer.parseInt(ZEILE[24+(fsMulti*2)]);
					String vPkwStatus = ZEILE[25+(fsMulti*2)];
					int vLkw = Integer.parseInt(ZEILE[30+(fsMulti*2)]);
					String vLkwStatus = ZEILE[31+(fsMulti*2)];
					int vgKfz = Integer.parseInt(ZEILE[36+(fsMulti*2)]);
					String vgKfzStatus = ZEILE[37+(fsMulti*2)];
					int b = Integer.parseInt(ZEILE[42+(fsMulti*2)]);
					String bStatus = ZEILE[43+(fsMulti*2)];
					long tNetto = Long.parseLong(ZEILE[46+(fsMulti*2)]) * 1000;
					String tNettoStatus = ZEILE[47+(fsMulti*2)];
					int sKfz = 1;

					datensatz.getTimeValue("T").setMillis(INTERVALL); //$NON-NLS-1$
					datensatz.getUnscaledValue("ArtMittelwertbildung").set(1); //$NON-NLS-1$
					datensatz = setAttribut("qKfz", qKfz, qKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("qPkw", qPkw, qPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("qLkw", qLkw, qLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vKfz", vKfz, vKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vPkw", vPkw, vPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vLkw", vLkw, vLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vgKfz", vgKfz, vgKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("b", b, bStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("tNetto", tNetto, tNettoStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("sKfz", sKfz, null, datensatz); //$NON-NLS-1$
				}catch(ArrayIndexOutOfBoundsException ex){
					datensatz = null;
				}
			}else{
				datensatz = null;
			}
		}
		
		return datensatz;
	}
	
	
	/**
	 * Setzt Attribut in Datensatz
	 * 
	 * @param attributName Name des Attributs
	 * @param wert Wert des Attributs
	 * @param datensatz der Datensatz
	 * @return der veränderte Datensatz
	 */
	private final Data setAttribut(final String attributName, long wert, String status, Data datensatz){
		Data data = datensatz;
	
		if(attributName.startsWith("v") && wert >= 255) {
			wert = -1;
		}
		
		int nErf = DUAKonstanten.NEIN;
		int wMax = DUAKonstanten.NEIN;
		int wMin = DUAKonstanten.NEIN;
		int wMaL = DUAKonstanten.NEIN;
		int wMiL = DUAKonstanten.NEIN;
		int impl = DUAKonstanten.NEIN;
		int intp = DUAKonstanten.NEIN;
		double guete = 1.0;
		
		int errCode = 0;
		
		if(status != null) {
			String[] splitStatus = status.trim().split(" ");
			
			for(int i = 0; i<splitStatus.length;i++) {
				if(splitStatus[i].equalsIgnoreCase("Fehl"))
					errCode = errCode-2;
				
				if(splitStatus[i].equalsIgnoreCase("nErm"))
					errCode = errCode-1;
				
				if(splitStatus[i].equalsIgnoreCase("Impl"))
					 impl = DUAKonstanten.JA;
				
				if(splitStatus[i].equalsIgnoreCase("Intp"))
					intp = DUAKonstanten.JA;				

				if(splitStatus[i].equalsIgnoreCase("nErf"))
					nErf = DUAKonstanten.JA;

				if(splitStatus[i].equalsIgnoreCase("wMaL"))
					wMaL = DUAKonstanten.JA;
				
				if(splitStatus[i].equalsIgnoreCase("wMax"))
					wMax = DUAKonstanten.JA;

				if(splitStatus[i].equalsIgnoreCase("wMiL"))
					wMiL = DUAKonstanten.JA;

				if(splitStatus[i].equalsIgnoreCase("wMin"))
					wMin = DUAKonstanten.JA;
				
				try {
//					guete = Float.parseFloat(splitStatus[i].replace(",", "."))*10000;
					guete = Float.parseFloat(splitStatus[i].replace(",", "."));
				} catch (Exception e) {
					//kein float Wert
				}
			}
		}
			
		if(errCode < 0)
			wert = errCode;

		DUAUtensilien.getAttributDatum(attributName + ".Wert", data).asUnscaledValue().set(wert); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.Erfassung.NichtErfasst", data).asUnscaledValue().set(nErf); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.PlFormal.WertMax", data).asUnscaledValue().set(wMax); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.PlFormal.WertMin", data).asUnscaledValue().set(wMin); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.PlLogisch.WertMaxLogisch", data).asUnscaledValue().set(wMaL); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.PlLogisch.WertMinLogisch", data).asUnscaledValue().set(wMiL); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.MessWertErsetzung.Implausibel", data).asUnscaledValue().set(impl); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Status.MessWertErsetzung.Interpoliert", data).asUnscaledValue().set(intp); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Güte.Index", data).asScaledValue().set(guete); //$NON-NLS-1$
		DUAUtensilien.getAttributDatum(attributName + ".Güte.Verfahren", data).asUnscaledValue().set(0); //$NON-NLS-1$
				
		return datensatz;
	}
}
