/*
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
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

package de.bsvrz.dua.dalve.util.para;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;

/**
 * Abstrakte Klasse zum Einlesen von Parametern aus der CSV-Datei innerhalb der Prüfspezifikation.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class ParaAnaProgImportMQAlsFS extends AbstractParaAnaProgImport {

	/**
	 * Standardkonstruktor.
	 *
	 * @param dav
	 *            Datenverteier-Verbindung
	 * @param objekt
	 *            das Systemobjekt, für das die Parameter gesetzt werden sollen
	 * @param csvQuelle
	 *            Quelle der Daten (CSV-Datei)
	 * @throws Exception
	 *             falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public ParaAnaProgImportMQAlsFS(final ClientDavInterface dav, final SystemObject[] objekt,
			final String csvQuelle) throws Exception {
		super(csvQuelle);
		if (DAV == null) {
			DAV = dav;
		}

		ATG_Analyse = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitAnalyseMq"); //$NON-NLS-1$
		ATG_PrognoseFlink = DAV.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseFlinkMq"); //$NON-NLS-1$
		ATG_PrognoseNormal = DAV.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseNormalMq"); //$NON-NLS-1$
		ATG_PrognoseTraege = DAV.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseTrägeMq"); //$NON-NLS-1$
		ATG_VLVERFAHREN1 = DAV.getDataModel().getAttributeGroup("atg.verkehrsLageVerfahren1");
		ATG_VLVERFAHREN2 = DAV.getDataModel().getAttributeGroup("atg.verkehrsLageVerfahren2");

		DD_Analyse = new DataDescription(ATG_Analyse, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_PrognoseFlink = new DataDescription(ATG_PrognoseFlink, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_PrognoseNormal = new DataDescription(ATG_PrognoseNormal, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_PrognoseTraege = new DataDescription(ATG_PrognoseTraege, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_Analyse = new DataDescription(ATG_Analyse, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_PrognoseFlink = new DataDescription(ATG_PrognoseFlink, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_PrognoseNormal = new DataDescription(ATG_PrognoseNormal, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_PrognoseTraege = new DataDescription(ATG_PrognoseTraege, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_VLVERFAHREN1 = new DataDescription(ATG_VLVERFAHREN1, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		DD_VLVERFAHREN2 = new DataDescription(ATG_VLVERFAHREN2, DAV.getDataModel().getAspect(
				DaVKonstanten.ASP_PARAMETER_VORGABE));

		this.objekt = objekt;
	}

	/**
	 * Führt den Parameterimport aus.
	 *
	 * @param index
	 *            der index
	 * @throws Exception
	 *             wenn die Parameter nicht vollständig importiert werden konnten
	 */
	@Override
	public void importiereParameterAnalyse(final int index) throws Exception {

		reset();
		getNaechsteZeile();

		SystemObject fsObjekt = null;
		for (final SystemObject sysObjekt : objekt) {
			if (sysObjekt.getName().endsWith("" + index)) { //$NON-NLS-1$
				fsObjekt = sysObjekt;
			}
		}

		DAV.subscribeSender(this, fsObjekt, DD_Analyse, SenderRole.sender());

		String[] zeile = null;

		final Data parameter = DAV.createData(ATG_Analyse);

		while ((zeile = getNaechsteZeile()) != null) {
			final String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$

			final String attPfadAnalyse = getAnalyseAttributPfadVon(attributInCSVDatei, index);
			if (attPfadAnalyse != null) {
				try {
					final long l = Long.parseLong(wert);
					// DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(l);
					DUAUtensilien.getAttributDatum(attPfadAnalyse, parameter).asScaledValue()
					.set(l);
				} catch (final NumberFormatException ex) {
					final double d = Double.parseDouble(wert);
					// DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(d);
					DUAUtensilien.getAttributDatum(attPfadAnalyse, parameter).asScaledValue()
					.set(d);
				}
			}
		}

		setParaAnalyseWichtung(parameter);

		final ResultData resultat = new ResultData(fsObjekt, new DataDescription(ATG_Analyse, DAV
				.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE)),
				System.currentTimeMillis(), parameter);
		DAV.sendData(resultat);

		DAV.unsubscribeSender(this, fsObjekt, DD_Analyse);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.bsvrz.dua.dalve.util.para.AbstractParaAnaProgImport#getAnalyseAttributPfadVon(java.lang
	 * .String, int)
	 */
	@Override
	protected String getAnalyseAttributPfadVon(final String attributInCSVDatei, final int index) {
		if (attributInCSVDatei.endsWith(")")) { //$NON-NLS-1$
			final String nummerStr = attributInCSVDatei.substring(attributInCSVDatei.length() - 2,
					attributInCSVDatei.length() - 1);
			int nummer = -1;
			try {
				nummer = Integer.parseInt(nummerStr);
			} catch (final Exception ex) {
				//
			}

			if (nummer == index) {
				if (attributInCSVDatei.startsWith("kKfzGrenz")) { //$NON-NLS-1$
					return "KKfz.Grenz"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kKfzMax")) { //$NON-NLS-1$
					return "KKfz.Max"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kLkwGrenz")) { //$NON-NLS-1$
					return "KLkw.Grenz"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kLkwMax")) { //$NON-NLS-1$
					return "KLkw.Max"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kPkwGrenz")) { //$NON-NLS-1$
					return "KPkw.Grenz"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kPkwMax")) { //$NON-NLS-1$
					return "KPkw.Max"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kBGrenz")) { //$NON-NLS-1$
					return "KB.Grenz"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("kBMax")) { //$NON-NLS-1$
					return "KB.Max"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("k1")) { //$NON-NLS-1$
					return "fl.k1"; //$NON-NLS-1$
				}
				if (attributInCSVDatei.startsWith("k2")) { //$NON-NLS-1$
					return "fl.k2"; //$NON-NLS-1$
				}
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.bsvrz.dua.dalve.util.para.AbstractParaAnaProgImport#setParaAnalyseWichtung(de.bsvrz.dav
	 * .daf.main.Data)
	 */
	@Override
	protected void setParaAnalyseWichtung(final Data parameter) {
		// VOID, Wichtung nur bei Messquerschnitten
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * de.bsvrz.dua.dalve.util.para.AbstractParaAnaProgImport#setParameterResult(de.bsvrz.dav.daf
	 * .main.Data, java.lang.String, java.lang.String)
	 */
	@Override
	protected void setParameterResult(final Data parameter, final String attPfad, final String wert) {
		final String[] atts = { "QKfz", "VKfz", "QLkw", "VLkw", "QPkw", "VPkw", "ALkw", "KKfz",
				"KLkw", "KPkw", "QB", "KB" };

		for (final String att : atts) {
			final String attPfadPrognose = att + attPfad;
			try {
				final long l = Long.parseLong(wert);
				DUAUtensilien.getAttributDatum(attPfadPrognose, parameter).asScaledValue().set(l);
			} catch (final NumberFormatException ex) {
				final double d = Double.parseDouble(wert);
				DUAUtensilien.getAttributDatum(attPfadPrognose, parameter).asScaledValue().set(d);
			}

		}
	}
}
