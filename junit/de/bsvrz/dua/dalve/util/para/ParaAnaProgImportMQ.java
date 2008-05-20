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
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;

/**
 * Abstrakte Klasse zum Einlesen von Parametern aus der CSV-Datei 
 * innerhalb der Pr�fspezifikation
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class ParaAnaProgImportMQ
extends AbstractParaAnaProgImport {
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteier-Verbindung
	 * @param objekt das Systemobjekt, f�r das die Parameter gesetzt werden sollen
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollst�ndig initialisiert werden konnte
	 */
	public ParaAnaProgImportMQ(final ClientDavInterface dav, 
								   final SystemObject[] objekt,
						   		   final String csvQuelle)
	throws Exception{
		super(csvQuelle);
		if(DAV == null){
			DAV = dav;
		}
		
		this.isMQ = true;
		
		ATG_Analyse = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitAnalyseMq"); //$NON-NLS-1$
		ATG_PrognoseFlink = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseFlinkMq"); //$NON-NLS-1$
		ATG_PrognoseNormal = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseNormalMq"); //$NON-NLS-1$
		ATG_PrognoseTraege = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseTr�geMq"); //$NON-NLS-1$
		ATG_VLVERFAHREN1 = DAV.getDataModel().getAttributeGroup("atg.verkehrsLageVerfahren1");
		ATG_VLVERFAHREN2 = DAV.getDataModel().getAttributeGroup("atg.verkehrsLageVerfahren2");
		
		ATG_VLVERFAHREN3 = DAV.getDataModel().getAttributeGroup("atg.verkehrsLageVerfahren3");
		ATG_FUNDAMENTALDIAGRAMM = DAV.getDataModel().getAttributeGroup("atg.fundamentalDiagramm");
		
		DD_Analyse = new DataDescription(
				ATG_Analyse, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_PrognoseFlink = new DataDescription(
				ATG_PrognoseFlink, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		
		DD_PrognoseNormal = new DataDescription(
				ATG_PrognoseNormal, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_PrognoseTraege = new DataDescription(
				ATG_PrognoseTraege, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);

		DD_VLVERFAHREN1 = new DataDescription(
				ATG_VLVERFAHREN1, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_VLVERFAHREN2 = new DataDescription(
				ATG_VLVERFAHREN2, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_VLVERFAHREN3 = new DataDescription(
				ATG_VLVERFAHREN3, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_FUNDAMENTALDIAGRAMM = new DataDescription(
				ATG_FUNDAMENTALDIAGRAMM, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		this.objekt = objekt;
	}

	@Override
	protected String getAnalyseAttributPfadVon(final String attributInCSVDatei, final int index) {
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
				if(attributInCSVDatei.startsWith("kKfzGrenz")){ //$NON-NLS-1$
					return "KKfz.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kKfzMax")){ //$NON-NLS-1$
					return "KKfz.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kLkwGrenz")){ //$NON-NLS-1$
					return "KLkw.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kLkwMax")){ //$NON-NLS-1$
					return "KLkw.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kPkwGrenz")){ //$NON-NLS-1$
					return "KPkw.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kPkwMax")){ //$NON-NLS-1$
					return "KPkw.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kBGrenz")){ //$NON-NLS-1$
					return "KB.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kBMax")){ //$NON-NLS-1$
					return "KB.Max"; //$NON-NLS-1$
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

	@Override
	protected void setParaAnalyseWichtung(Data parameter) {
		parameter.getItem("wichtung").asArray().setLength(0);
	}

	@Override
	protected void setParameterResult(Data parameter, String attPfad, String wert) {
		String[] atts = {"QKfz", "VKfz", "QLkw", "VLkw", "QPkw", "VPkw", "ALkw", "KKfz", "KLkw", "KPkw", "QB", "KB"};
		
		for(String att : atts) {
			String attPfadPrognose = att+attPfad;
			try{
				long l = Long.parseLong(wert);
				DUAUtensilien.getAttributDatum(attPfadPrognose, parameter).asScaledValue().set(l);
			}catch(NumberFormatException ex){
				double d = Double.parseDouble(wert);
				DUAUtensilien.getAttributDatum(attPfadPrognose, parameter).asScaledValue().set(d);
			}
			
		}
	}
}
