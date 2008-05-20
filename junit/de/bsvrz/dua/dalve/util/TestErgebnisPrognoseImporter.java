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

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;


/**
 * Liest die Ausgangsdaten für die Prüfung der Datenaufbereitung LVE ein
 * 
 * @author BitCtrl Systems GmbH, Görlitz
 *
 */
public class TestErgebnisPrognoseImporter
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
	protected static long INTERVALL = Constants.MILLIS_PER_MINUTE;
	
	/**
	 * Identifiziert einen Wert vom Typ Prognose
	 */
	private static final String TYP_PROGNOSEWERT = "P";
	
	/**
	 * Identifiziert einen Wert vom Typ Geglättet
	 */
	private static final String TYP_GLATTWERT = "G";
	
	/**
	 * Repräsentiert den Modus Flink
	 */
	private static final int MODUS_FLINK = 0;
	
	/**
	 * Repräsentiert den Modus Normal
	 */
	private static final int MODUS_NORMAL = 1;
	
	/**
	 * Repräsentiert den Modus Träge
	 */
	private static final int MODUS_TRAEGE = 2;

	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteier-Verbindung
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public TestErgebnisPrognoseImporter(final ClientDavInterface dav, 
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
	 * Liefert einen Prognosedatensatz unter dem Aspekt Flink
	 * @return Einen Prognosedatensatz unter dem Aspekt Flink
	 */
	public final Data getFSPrognoseFlinkDatensatz() {
		return getFSProgGlattDatensatz(TYP_PROGNOSEWERT, MODUS_FLINK);
	}
	
	/**
	 * Liefert einen Prognosedatensatz unter dem Aspekt Normal
	 * @return Einen Prognosedatensatz unter dem Aspekt Normal
	 */
	public final Data getFSPrognoseNormalDatensatz() {
		return getFSProgGlattDatensatz(TYP_PROGNOSEWERT, MODUS_NORMAL);
	}
	
	/**
	 * Liefert einen Prognosedatensatz unter dem Aspekt Träge
	 * @return Einen Prognosedatensatz unter dem Aspekt Träge
	 */
	public final Data getFSPrognoseTraegeDatensatz() {
		return getFSProgGlattDatensatz(TYP_PROGNOSEWERT, MODUS_TRAEGE);
	}
	
	/**
	 * Liefert einen Datensatz mit geglätteten Werten unter dem Aspekt Flink
	 * @return Einen Datensatz mit geglätteten Werten unter dem Aspekt Flink
	 */
	public final Data getFSGeglaettetFlinkDatensatz() {
		return getFSProgGlattDatensatz(TYP_GLATTWERT, MODUS_FLINK);
	}
	
	/**
	 * Liefert einen Datensatz mit geglätteten Werten unter dem Aspekt Normal
	 * @return Einen Datensatz mit geglätteten Werten unter dem Aspekt Normal
	 */
	public final Data getFSGeglaettetNormalDatensatz() {
		return getFSProgGlattDatensatz(TYP_GLATTWERT, MODUS_NORMAL);
	}
	
	/**
	 * Liefert einen Datensatz mit geglätteten Werten unter dem Aspekt Träge
	 * @return Einen Datensatz mit geglätteten Werten unter dem Aspekt Träge
	 */
	public final Data getFSGeglaettetTraegeDatensatz() {
		return getFSProgGlattDatensatz(TYP_GLATTWERT, MODUS_TRAEGE);
	}
	
	/**
	 * Bildet einen Ausgabe-Datensatz der FS-Prognose/Analysewerte aus den Daten der aktuellen CSV-Zeile
	 * 
	 * @param attPraefix Entweder <code>P</code> für einen Prognosedatensatz oder <code>G</code>
	 * für einen Datensatz mit geglätteten Werten
	 * @return ein Datensatz der übergebenen Parametern mit den Daten der nächsten Zeile
	 * oder <code>null</code>, wenn der Dateizeiger am Ende ist
	 */	
	private final Data getFSProgGlattDatensatz(final String attPraefix, final int mode){	
		Data datensatz;
		int offset;
		
		if(attPraefix.equals("P")) {
			datensatz = DAV.createData(DAV.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_FS)); //$NON-NLS-1$
			offset = 0;
		} else {
			datensatz = DAV.createData(DAV.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_FS)); //$NON-NLS-1$
			offset = 1;
		}

		if(datensatz != null){
			
			if(ZEILE != null){
				try{
					int qB = Integer.parseInt(ZEILE[2+offset+(mode*2)]);
					int vKfz = Integer.parseInt(ZEILE[8+offset+(mode*2)]);
					
					int nErmFehl = DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT;	
					
					int kB;					
					if(offset == 1) {
						kB = Integer.parseInt(ZEILE[14+mode]);
					} else {
						kB = nErmFehl;
					}
					
					datensatz.getTimeValue("T").setMillis(INTERVALL); //$NON-NLS-1$
					datensatz = setAttribut("qKfz"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("qPkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("qLkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vKfz"+attPraefix, vKfz, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vPkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("vLkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("aLkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("kKfz"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("kLkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("kPkw"+attPraefix, nErmFehl, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("qB"+attPraefix, qB, null, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("kB"+attPraefix, kB, null, datensatz); //$NON-NLS-1$

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
	
		if((attributName.startsWith("v") || attributName.startsWith("V")) //$NON-NLS-1$ //$NON-NLS-2$
				&& wert >= 255) {
			wert = -3;
		}
		
		if((attributName.startsWith("k") || attributName.startsWith("K")) //$NON-NLS-1$ //$NON-NLS-2$
				&& wert > 10000) {
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
			String[] splitStatus = status.trim().split(" "); //$NON-NLS-1$
			
			for(int i = 0; i<splitStatus.length;i++) {
				if(splitStatus[i].equalsIgnoreCase("Fehl")) //$NON-NLS-1$
					errCode = errCode-2;
				
				if(splitStatus[i].equalsIgnoreCase("nErm")) //$NON-NLS-1$
					errCode = errCode-1;
				
				if(splitStatus[i].equalsIgnoreCase("Impl")) //$NON-NLS-1$
					 impl = DUAKonstanten.JA;
				
				if(splitStatus[i].equalsIgnoreCase("Intp")) //$NON-NLS-1$
					intp = DUAKonstanten.JA;				

				if(splitStatus[i].equalsIgnoreCase("nErf")) //$NON-NLS-1$
					nErf = DUAKonstanten.JA;

				if(splitStatus[i].equalsIgnoreCase("wMaL")) //$NON-NLS-1$
					wMaL = DUAKonstanten.JA;
				
				if(splitStatus[i].equalsIgnoreCase("wMax")) //$NON-NLS-1$
					wMax = DUAKonstanten.JA;

				if(splitStatus[i].equalsIgnoreCase("wMiL")) //$NON-NLS-1$
					wMiL = DUAKonstanten.JA;

				if(splitStatus[i].equalsIgnoreCase("wMin")) //$NON-NLS-1$
					wMin = DUAKonstanten.JA;
				
				try {
//					guete = Float.parseFloat(splitStatus[i].replace(",", "."))*10000;
					guete = Float.parseFloat(splitStatus[i].replace(",", ".")); //$NON-NLS-1$ //$NON-NLS-2$
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
