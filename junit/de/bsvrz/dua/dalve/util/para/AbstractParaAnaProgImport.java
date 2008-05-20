/** 
 * Segment 4 Daten�bernahme und Aufbereitung (DUA)
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
 * Wei�enfelser Stra�e 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */

package de.bsvrz.dua.dalve.util.para;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;

/**
 * Abstrakte Klasse zum Einlesen von Parametern aus der CSV-Datei 
 * innerhalb der Pr�fspezifikation
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public abstract class AbstractParaAnaProgImport
extends CSVImporter
implements ClientSenderInterface {
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;
	
	/**
	 * Systemobjekt, f�r das die Parameter gesetzt werden sollen
	 */
	protected SystemObject[] objekt = null;
	
	/**
	 * Attributgruppe der Analyse-Parameter
	 */
	protected AttributeGroup ATG_Analyse;

	/**
	 * Attributgruppe der Prognose-Parameter (Flink)
	 */
	protected AttributeGroup ATG_PrognoseFlink;
	
	/**
	 * Attributgruppe der Prognose-Parameter (Normal)
	 */
	protected AttributeGroup ATG_PrognoseNormal;
	
	/**
	 * Attributgruppe der Prognose-Parameter (Tr�ge)
	 */
	protected AttributeGroup ATG_PrognoseTraege;

	/**
	 * Attributgruppe des Verkehrslageverfahren 1
	 */
	protected AttributeGroup ATG_VLVERFAHREN1;
	
	/**
	 * Attributgruppe des Verkehrslageverfahren 2
	 */
	protected AttributeGroup ATG_VLVERFAHREN2;
	
	/**
	 * Attributgruppe des Verkehrslageverfahren 3
	 */
	protected AttributeGroup ATG_VLVERFAHREN3;

	/**
	 * Attributgruppe des Fundamentaldiagramm
	 */
	protected AttributeGroup ATG_FUNDAMENTALDIAGRAMM;
	
	/**
	 * Datenbeschreibung Analyse
	 */
	protected DataDescription DD_Analyse;

	/**
	 * Datenbeschreibung Prognose (Flink)
	 */
	protected DataDescription DD_PrognoseFlink;
	
	/**
	 * Datenbeschreibung Prognose (Normal)
	 */
	protected DataDescription DD_PrognoseNormal;
	
	/**
	 * Datenbeschreibung Prognose (Tr�ge)
	 */
	protected DataDescription DD_PrognoseTraege;

	/**
	 * Datenbeschreibung Verkehrslageverfahren 1
	 */
	protected DataDescription DD_VLVERFAHREN1;
	
	/**
	 * Datenbeschreibung Verkehrslageverfahren 2
	 */
	protected DataDescription DD_VLVERFAHREN2;
	
	/**
	 * Datenbeschreibung Verkehrslageverfahren 3
	 */
	protected DataDescription DD_VLVERFAHREN3;
	
	/**
	 * Datenbeschreibung Fundamentaldiagramm
	 */
	protected DataDescription DD_FUNDAMENTALDIAGRAMM;
	
	/**
	 * Gibt an, ob dies der Parameterimporter eines Messquerschnitts ist
	 */
	protected boolean isMQ = false;
	
	/**
	 * Standardkonstruktor
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollst�ndig initialisiert werden konnte
	 */
	public AbstractParaAnaProgImport(final String csvQuelle)
	throws Exception {
		super(csvQuelle);
	}
	
	/**
	 * F�hrt den Parameterimport aus
	 * 
	 * @param index der index
	 * @throws Exception wenn die Parameter nicht vollst�ndig
	 * importiert werden konnten
	 */
	public final void importiereParameterAnalyse(int index)
	throws Exception{
		
		this.reset();
		this.getNaechsteZeile();
		
		SystemObject fsObjekt = null;
		for(SystemObject sysObjekt : objekt) {
			if(sysObjekt.getName().endsWith("."+index)) { //$NON-NLS-1$
				fsObjekt = sysObjekt;
			}
		}
		
		DAV.subscribeSender(this, fsObjekt,DD_Analyse, SenderRole.sender());
		
		String[] zeile = null;
		
		Data parameter = DAV.createData(ATG_Analyse);
		
		while( (zeile = this.getNaechsteZeile()) != null ){
			String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
			
			String attPfadAnalyse = getAnalyseAttributPfadVon(attributInCSVDatei, index);
			if(attPfadAnalyse != null){
				try{
					long l = Long.parseLong(wert);
					//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(l);
					DUAUtensilien.getAttributDatum(attPfadAnalyse, parameter).asScaledValue().set(l);
				}catch(NumberFormatException ex){
					double d = Double.parseDouble(wert);
					//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(d);
					DUAUtensilien.getAttributDatum(attPfadAnalyse, parameter).asScaledValue().set(d);
				}
			}
		}
		
		setParaAnalyseWichtung(parameter);
		
		ResultData resultat = new ResultData(fsObjekt, new DataDescription(
				ATG_Analyse, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameter);
		DAV.sendData(resultat);
		
		DAV.unsubscribeSender(this, fsObjekt, DD_Analyse);
	}

	/**
	 * F�hrt den Parameterimport aus
	 * 
	 * @param index der index
	 * @throws Exception wenn die Parameter nicht vollst�ndig
	 * importiert werden konnten
	 */
	public final void importiereParameterPrognose()
	throws Exception{
		SystemObject obj = objekt[0];
		this.reset();
		this.getNaechsteZeile();
		
		DAV.subscribeSender(this, obj,DD_PrognoseFlink, SenderRole.sender());
		DAV.subscribeSender(this, obj,DD_PrognoseNormal, SenderRole.sender());
		DAV.subscribeSender(this, obj,DD_PrognoseTraege, SenderRole.sender());
		
		String[] zeile = null;
		
		Data parameterProgFlink = DAV.createData(ATG_PrognoseFlink);
		Data parameterProgNorm = DAV.createData(ATG_PrognoseNormal);
		Data parameterProgTraege = DAV.createData(ATG_PrognoseTraege);
		
		String qStartwert = null;
		String vStartwert = null;
		
		while( (zeile = this.getNaechsteZeile()) != null ){
			String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
			
			//Prognose Flink
			String attPfadPFlink = getPrognoseAttributPfadVon(attributInCSVDatei, "flink");
			if(attPfadPFlink != null){
				setParameterResult(parameterProgFlink, attPfadPFlink, wert);
			}
			
			//Prognose Normal
			String attPfadPNormal = getPrognoseAttributPfadVon(attributInCSVDatei, "normal");
			if(attPfadPNormal != null){
				setParameterResult(parameterProgNorm, attPfadPNormal, wert);
			}
			
			//Prognose Tr�ge
			String attPfadPTraege = getPrognoseAttributPfadVon(attributInCSVDatei, "tr�ge");
			if(attPfadPTraege != null){
				setParameterResult(parameterProgTraege, attPfadPTraege, wert);
			}
			
			if(attributInCSVDatei.startsWith("ZAltStartQ")) {
				qStartwert = wert;
			}
			
			if(attributInCSVDatei.startsWith("ZAltStartV")) {
				vStartwert = wert;
			}
			
			if(qStartwert != null && vStartwert != null) {
				setPrognoseStartwerte(parameterProgFlink, qStartwert, vStartwert);
				setPrognoseStartwerte(parameterProgNorm, qStartwert, vStartwert);
				setPrognoseStartwerte(parameterProgTraege, qStartwert, vStartwert);
			}
		}
		
		ResultData resultatFlink = new ResultData(obj, new DataDescription(
				ATG_PrognoseFlink, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterProgFlink);
		
		ResultData resultatNormal = new ResultData(obj, new DataDescription(
				ATG_PrognoseNormal, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterProgNorm);
		
		ResultData resultatTraege = new ResultData(obj, new DataDescription(
				ATG_PrognoseTraege, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterProgTraege);
		
		DAV.sendData(resultatFlink);
		DAV.sendData(resultatNormal);
		DAV.sendData(resultatTraege);
		
		DAV.unsubscribeSender(this, obj, DD_PrognoseFlink);
		DAV.unsubscribeSender(this, obj, DD_PrognoseNormal);
		DAV.unsubscribeSender(this, obj, DD_PrognoseTraege);
	}
	
	/**
	 * F�hrt den Parameterimport aus
	 * 
	 * @param index der index
	 * @throws Exception wenn die Parameter nicht vollst�ndig
	 * importiert werden konnten
	 */
	public final void importiereParameterStoerfall(int index)
	throws Exception{
		SystemObject obj = objekt[0];
		this.reset();
		this.getNaechsteZeile();
		
		DAV.subscribeSender(this, obj,DD_VLVERFAHREN1, SenderRole.sender());
		DAV.subscribeSender(this, obj,DD_VLVERFAHREN2, SenderRole.sender());

		Data parameterVLV1 = DAV.createData(ATG_VLVERFAHREN1);
		Data parameterVLV2 = DAV.createData(ATG_VLVERFAHREN2);
		
		Data parameterVLV3 = null;
		Data parameterFD = null;
		if(isMQ) {
			DAV.subscribeSender(this, obj,DD_VLVERFAHREN3, SenderRole.sender());
			DAV.subscribeSender(this, obj,DD_FUNDAMENTALDIAGRAMM, SenderRole.sender());
			parameterVLV3 = DAV.createData(ATG_VLVERFAHREN3);
			parameterFD = DAV.createData(ATG_FUNDAMENTALDIAGRAMM);
		}
		
		String[] zeile = null;
		
		while( (zeile = this.getNaechsteZeile()) != null ){
			String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
			
			String attPfadStoer = getStoerfallAttributPfadVon(attributInCSVDatei);
			if(attPfadStoer != null){
				try{
					long l = Long.parseLong(wert);

					if(!attPfadStoer.endsWith("Hysterese")) {
						DUAUtensilien.getAttributDatum(attPfadStoer, parameterVLV2).asScaledValue().set(l);
						if(!attPfadStoer.endsWith("3") && !attPfadStoer.endsWith("T")) {
							DUAUtensilien.getAttributDatum(attPfadStoer, parameterVLV1).asScaledValue().set(l);
						}
					}

					if(isMQ) {
						DUAUtensilien.getAttributDatum(attPfadStoer, parameterVLV3).asScaledValue().set(l);
					}
				}catch(NumberFormatException ex){
					
				}
				
			}
			
			if(isMQ) {
				String attPfadStoerFD = getFDAttributPfadVon(attributInCSVDatei, index);
				if(attPfadStoerFD != null){
					try{
						long l = Long.parseLong(wert);
						DUAUtensilien.getAttributDatum(attPfadStoerFD, parameterFD).asScaledValue().set(l);
					}catch(NumberFormatException ex){
						
					}
					
				}
			}
		}
		
		ResultData resultatVLV1 = new ResultData(obj, new DataDescription(
				ATG_VLVERFAHREN1, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterVLV1);
		
		ResultData resultatVLV2 = new ResultData(obj, new DataDescription(
				ATG_VLVERFAHREN2, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterVLV2);
		
		DAV.sendData(resultatVLV1);
		DAV.sendData(resultatVLV2);

		DAV.unsubscribeSender(this, obj, DD_VLVERFAHREN1);
		DAV.unsubscribeSender(this, obj, DD_VLVERFAHREN2);
		
		ResultData resultatVLV3 = null;
		ResultData resultatFD = null;
		if(isMQ) {
			resultatVLV3 = new ResultData(obj, new DataDescription(
					ATG_VLVERFAHREN3, 
					DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
					(short)0), System.currentTimeMillis(), parameterVLV3);
			DAV.sendData(resultatVLV3);
			
			resultatFD = new ResultData(obj, new DataDescription(
					ATG_FUNDAMENTALDIAGRAMM, 
					DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
					(short)0), System.currentTimeMillis(), parameterFD);
			DAV.sendData(resultatFD);
			
			DAV.unsubscribeSender(this, obj, DD_VLVERFAHREN3);
			DAV.unsubscribeSender(this, obj, DD_FUNDAMENTALDIAGRAMM);
		}	

	}
	
	
	/**
	 * Erfragt den Attributpfad zu einem Analyse-Attribut, das in der CSV-Datei 
	 * den �bergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	abstract protected String getAnalyseAttributPfadVon(final String attributInCSVDatei, final int index);
	
	/**
	 * Erfragt den Attributpfad zu einem Prognose-Attribut, das in der CSV-Datei 
	 * den �bergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	protected String getPrognoseAttributPfadVon(final String attributInCSVDatei, final String prognoseTyp) {

		if(attributInCSVDatei.startsWith("alpha1"+prognoseTyp)){ //$NON-NLS-1$
			return ".alpha1"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("alpha2"+prognoseTyp)){ //$NON-NLS-1$
			return ".alpha2"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("beta1"+prognoseTyp)){ //$NON-NLS-1$
			return ".beta1"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("beta2"+prognoseTyp)){ //$NON-NLS-1$
			return ".beta2"; //$NON-NLS-1$
		}
		
		return null;
	}
	
	/**
	 * Erfragt den Attributpfad zu einem Stoerfall-Attribut, das in der CSV-Datei 
	 * den �bergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	protected String getStoerfallAttributPfadVon(final String attributInCSVDatei) {

		if(attributInCSVDatei.startsWith("v1")){ //$NON-NLS-1$
			return "v1"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("v2")){ //$NON-NLS-1$
			return "v2"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("k1")){ //$NON-NLS-1$
			return "k1"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("k2")){ //$NON-NLS-1$
			return "k2"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("k3")){ //$NON-NLS-1$
			return "k3"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("k5T")){ //$NON-NLS-1$
			return "kT"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("vst5Hysterese")){ //$NON-NLS-1$
			return "VST5Hysterese"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("vst6Hysterese")){ //$NON-NLS-1$
			return "VST6Hysterese"; //$NON-NLS-1$
		}
		
		return null;
	}
	
	/**
	 * Erfragt den Attributpfad zu einem Fundamentaldiagramm-Attribut, das in der
	 * CSV-Datei den �bergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	protected String getFDAttributPfadVon(final String attributInCSVDatei, final int index) {
		if(attributInCSVDatei.endsWith(")")){ //$NON-NLS-1$
			String nummerStr = attributInCSVDatei.substring(
					attributInCSVDatei.length() - 2, attributInCSVDatei.length() - 1);
			int nummer = -1;
			try {
				nummer = Integer.parseInt(nummerStr);
			} catch(Exception ex) {
				//
			}

			if(nummer == index) {
				if(attributInCSVDatei.startsWith("Q0")){ //$NON-NLS-1$
					return "Q0"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("K0")){ //$NON-NLS-1$
					return "K0"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("V0")){ //$NON-NLS-1$
					return "V0"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("VFrei")){ //$NON-NLS-1$
					return "VFrei"; //$NON-NLS-1$
				}
			}
		}

		return null;
	}
	
	abstract protected void setParameterResult(final Data parameter, final String attPfad, final String wert);
	
	protected void setPrognoseStartwerte(final Data parameter, final String wertQ, final String wertV) {
		String[] atts = {"KfzStart", "LkwStart", "PkwStart"};
		
		for(String att : atts) {
			String attPfadQ = isMQ ? "Q"+att : "q"+att;
			String attPfadV = isMQ ? "V"+att : "v"+att;
			String attPfadK = isMQ ? "K"+att : "k"+att;

			try{
				long l = Long.parseLong(wertQ);
				DUAUtensilien.getAttributDatum(attPfadQ, parameter).asScaledValue().set(l);
			}catch(NumberFormatException ex){
				double d = Double.parseDouble(wertQ);
				DUAUtensilien.getAttributDatum(attPfadQ, parameter).asScaledValue().set(d);
			}
			
			try{
				long l = Long.parseLong(wertV);
				DUAUtensilien.getAttributDatum(attPfadV, parameter).asScaledValue().set(l);
			}catch(NumberFormatException ex){
				double d = Double.parseDouble(wertV);
				DUAUtensilien.getAttributDatum(attPfadV, parameter).asScaledValue().set(d);
			}
			
			DUAUtensilien.getAttributDatum(attPfadK, parameter).asUnscaledValue().set(0);	
			
		}
		
		String aLkwStart;
		String qBStart;
		String kBStart;
		if(!isMQ) {
			aLkwStart = "aLkwStart";
			qBStart = "qBStart";
			kBStart = "kBStart";
		} else {
			aLkwStart = "ALkwStart";
			qBStart = "QBStart";
			kBStart = "KBStart";			
		}
		
		
		DUAUtensilien.getAttributDatum(aLkwStart, parameter).asUnscaledValue().set(0);
		
		
		try{
			long l = Long.parseLong(wertQ);
			DUAUtensilien.getAttributDatum(qBStart, parameter).asScaledValue().set(l);
		}catch(NumberFormatException ex){
			double d = Double.parseDouble(wertQ);
			DUAUtensilien.getAttributDatum(qBStart, parameter).asScaledValue().set(d);
		}
		
		DUAUtensilien.getAttributDatum(kBStart, parameter).asUnscaledValue().set(0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		// keine �berpr�fung
	}
	

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}
	
	/**
	 * Setzt das Attribut Wichtung im �bergebenen Parameterdatensatz
	 * @param parameter Der Datensatz, in dem das Attribut Wichtung gesetzt werden soll
	 */
	abstract protected void setParaAnalyseWichtung(final Data parameter);
	
}