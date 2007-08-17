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

package de.bsvrz.dua.dalve.para;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientSenderInterface;
import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.configuration.interfaces.AttributeGroup;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.dua.dalve.util.CSVImporter;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;


/**
 * Abstrakte Klasse zum Einlesen von Parametern aus der CSV-Datei 
 * innerhalb der Prüfspezifikation
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class ParaAnaProgImport
extends CSVImporter
implements ClientSenderInterface {
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;
	
	/**
	 * Systemobjekt, für das die Parameter gesetzt werden sollen
	 */
	protected SystemObject[] objekt = null;
	
	/**
	 * Attributgruppe der FS-Analyse-Parameter
	 */
	AttributeGroup ATG_FSAnalyse;
	
	/**
	 * Datenbeschreibung Logisch
	 */
	private DataDescription DD_FSAnalyse;
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteier-Verbindung
	 * @param objekt das Systemobjekt, für das die Parameter gesetzt werden sollen
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public ParaAnaProgImport(final ClientDavInterface dav, 
								   final SystemObject[] objekt,
						   		   final String csvQuelle)
	throws Exception{
		super(csvQuelle);
		if(DAV == null){
			DAV = dav;
		}
		
		ATG_FSAnalyse = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitAnalyseFs");
		
		DD_FSAnalyse = new DataDescription(
				ATG_FSAnalyse, 
				DAV.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_VORGABE),
				(short)0);
		
		this.objekt = objekt;
	}
	
		
	/**
	 * Führt den Parameterimport aus
	 * 
	 * @param int index
	 * @throws Exception wenn die Parameter nicht vollständig
	 * importiert werden konnten
	 */
	public final void importiereParameter(int index)
	throws Exception{
		
		this.reset();
		this.getNaechsteZeile();
		
		SystemObject fsObjekt = null;
		for(SystemObject sysObjekt : objekt) {
			if(sysObjekt.getName().endsWith("."+index)) {
				fsObjekt = sysObjekt;
			}
		}
		
		DAV.subscribeSender(this, fsObjekt,DD_FSAnalyse, SenderRole.sender());
		
		String[] zeile = null;
		
		Data parameter = DAV.createData(ATG_FSAnalyse);
		
		while( (zeile = this.getNaechsteZeile()) != null ){
			String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", ".");
			String attPfad = getAttributPfadVon(attributInCSVDatei, index);
			if(attPfad != null){
				try{
					long l = Long.parseLong(wert);
					//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(l);
					DUAUtensilien.getAttributDatum(attPfad, parameter).asScaledValue().set(l);
				}catch(NumberFormatException ex){
					double d = Double.parseDouble(wert);
					//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(d);
					DUAUtensilien.getAttributDatum(attPfad, parameter).asScaledValue().set(d);
				}
			}
		}
		
		ResultData resultat = new ResultData(fsObjekt, new DataDescription(
				ATG_FSAnalyse, 
				DAV.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameter);
		DAV.sendData(resultat);
		
		DAV.unsubscribeSender(this, fsObjekt, DD_FSAnalyse);
	}

	/**
	 * Erfragt den Attributpfad zu einem Attribut, das in der CSV-Datei 
	 * den übergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	protected String getAttributPfadVon(final String attributInCSVDatei,
												 final int index) {

		if(attributInCSVDatei.endsWith(")")){ //$NON-NLS-1$
			String nummerStr = attributInCSVDatei.substring(
					attributInCSVDatei.length() - 2, attributInCSVDatei.length() - 1);
			int nummer = -1;
			try{
				nummer = Integer.parseInt(nummerStr);
			}catch(Exception ex){
				//
			}

			if(nummer == index){
				if(attributInCSVDatei.startsWith("kKfzGrenz")){ //$NON-NLS-1$
					return "kKfz.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kKfzMax")){ //$NON-NLS-1$
					return "kKfz.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kLkwGrenz")){ //$NON-NLS-1$
					return "kLkw.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kLkwMax")){ //$NON-NLS-1$
					return "kLkw.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kPkwGrenz")){ //$NON-NLS-1$
					return "kPkw.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kPkwMax")){ //$NON-NLS-1$
					return "kPkw.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kBGrenz")){ //$NON-NLS-1$
					return "kB.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kBMax")){ //$NON-NLS-1$
					return "kB.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("k1")){ //$NON-NLS-1$
					return "fl.k1"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("k2")){ //$NON-NLS-1$
					return "fl.k2"; //$NON-NLS-1$
				}
			}
		}
		
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		// keine Überprüfung
	}
	

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}
	
}
