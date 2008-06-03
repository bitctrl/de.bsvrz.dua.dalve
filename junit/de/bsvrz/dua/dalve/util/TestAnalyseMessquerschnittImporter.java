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
public class TestAnalyseMessquerschnittImporter
extends CSVImporter{
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;
	
	/**
	 * Hält aktuelle Daten des MQ 1-3
	 */
	protected String ZEILE[];
	
	/**
	 * T
	 */
	protected static long INTERVALL = Constants.MILLIS_PER_MINUTE;
	

	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteier-Verbindung
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public TestAnalyseMessquerschnittImporter(final ClientDavInterface dav, 
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
	 * @return ein Datensatz der übergebenen Attributgruppe mit den Daten der nächsten Zeile
	 * oder <code>null</code>, wenn der Dateizeiger am Ende ist
	 */	
	public final Data getDatensatz(){	
		Data datensatz = DAV.createData(DAV.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ));

		if(datensatz != null){
			
			if(ZEILE != null){
				try{
//					int fsMulti = 0;
//					int QKfz = Integer.parseInt(ZEILE[0+(fsMulti*2)]);
//					String QKfzStatus = ZEILE[1+(fsMulti*2)];
//					int QPkw = Integer.parseInt(ZEILE[6+(fsMulti*2)]);
//					String QPkwStatus = ZEILE[7+(fsMulti*2)];
//					int QLkw = Integer.parseInt(ZEILE[12+(fsMulti*2)]);
//					String QLkwStatus = ZEILE[13+(fsMulti*2)];
//					int VKfz = Integer.parseInt(ZEILE[18+(fsMulti*2)]);
//					String VKfzStatus = ZEILE[19+(fsMulti*2)];
//					int VPkw = Integer.parseInt(ZEILE[24+(fsMulti*2)]);
//					String VPkwStatus = ZEILE[25+(fsMulti*2)];
//					int VLkw = Integer.parseInt(ZEILE[30+(fsMulti*2)]);
//					String VLkwStatus = ZEILE[31+(fsMulti*2)];
//					int VgKfz = Integer.parseInt(ZEILE[36+(fsMulti*2)]);
//					String VgKfzStatus = ZEILE[37+(fsMulti*2)];
//					int B = Integer.parseInt(ZEILE[42+(fsMulti*2)]);
//					String BStatus = ZEILE[43+(fsMulti*2)];
//					int ALkw = Integer.parseInt(ZEILE[48+(fsMulti*2)]);
//					String ALkwStatus = ZEILE[49+(fsMulti*2)];
//					int KKfz = Integer.parseInt(ZEILE[54+(fsMulti*2)]);
//					String KKfzStatus = ZEILE[55+(fsMulti*2)];
//					int KLkw = Integer.parseInt(ZEILE[60+(fsMulti*2)]);
//					String KLkwStatus = ZEILE[61+(fsMulti*2)];
//					int KPkw = Integer.parseInt(ZEILE[66+(fsMulti*2)]);
//					String KPkwStatus = ZEILE[67+(fsMulti*2)];
//					int QB = Integer.parseInt(ZEILE[72+(fsMulti*2)]);
//					String QBStatus = ZEILE[73+(fsMulti*2)];
//					int KB = Integer.parseInt(ZEILE[78+(fsMulti*2)]);
//					String KBStatus = ZEILE[79+(fsMulti*2)];
//					int SKfz = 1;
//					String SKfzStatus = null;
//					int BMax = 0;
//					String BMaxStatus = null;
//					int VDelta = 0;
//					String VDeltaStatus = null;
					
					int QKfz = Integer.parseInt(ZEILE[84]);
					String QKfzStatus = ZEILE[85];
					int QLkw = Integer.parseInt(ZEILE[86]);
					String QLkwStatus = ZEILE[87];
					int QPkw = Integer.parseInt(ZEILE[88]);
					String QPkwStatus = ZEILE[89];
					int VKfz = Integer.parseInt(ZEILE[90]);
					String VKfzStatus = ZEILE[91];
					int VLkw = Integer.parseInt(ZEILE[92]);
					String VLkwStatus = ZEILE[93];
					int VPkw = Integer.parseInt(ZEILE[94]);
					String VPkwStatus = ZEILE[95];
					int VgKfz = Integer.parseInt(ZEILE[96]);
					String VgKfzStatus = ZEILE[97];
					int B = Integer.parseInt(ZEILE[98]);
					String BStatus = ZEILE[99];
					int BMax = Integer.parseInt(ZEILE[100]);
					String BMaxStatus = ZEILE[101];
					int SKfz = Integer.parseInt(ZEILE[102]);
					String SKfzStatus = ZEILE[103];
					int ALkw = Integer.parseInt(ZEILE[102]);
					String ALkwStatus = ZEILE[103];
					int KKfz = Integer.parseInt(ZEILE[106]);
					String KKfzStatus = ZEILE[107];
					int KLkw = Integer.parseInt(ZEILE[108]);
					String KLkwStatus = ZEILE[109];
					int KPkw = Integer.parseInt(ZEILE[110]);
					String KPkwStatus = ZEILE[111];
					int QB = Integer.parseInt(ZEILE[112]);
					String QBStatus = ZEILE[113];
					int KB = Integer.parseInt(ZEILE[114]);
					String KBStatus = ZEILE[115];
					int VDelta = Integer.parseInt(ZEILE[116]);
					String VDeltaStatus = ZEILE[117];
					
					datensatz = setAttribut("QKfz", QKfz, QKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("QPkw", QPkw, QPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("QLkw", QLkw, QLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VKfz", VKfz, VKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VPkw", VPkw, VPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VLkw", VLkw, VLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VgKfz", VgKfz, VgKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("B", B, BStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("BMax", BMax, BMaxStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("ALkw", ALkw, ALkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KKfz", KKfz, KKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KLkw", KLkw, KLkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KPkw", KPkw, KPkwStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("QB", QB, QBStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("KB", KB, KBStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("SKfz", SKfz, SKfzStatus, datensatz); //$NON-NLS-1$
					datensatz = setAttribut("VDelta", VDelta, VDeltaStatus, datensatz); //$NON-NLS-1$
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
	
		if(attributName.startsWith("V") && wert >= 255) { //$NON-NLS-1$
			wert = -1;
		}
		
		if(attributName.startsWith("K") && wert > 10000) { //$NON-NLS-1$
			wert = -1;
		}
		
		if(attributName.startsWith("B") && wert > 100) { //$NON-NLS-1$
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
