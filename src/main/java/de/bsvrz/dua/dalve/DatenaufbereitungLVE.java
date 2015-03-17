/**
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
package de.bsvrz.dua.dalve;

import java.util.Collection;
import java.util.HashSet;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.analyse.FsAnalyseModul;
import de.bsvrz.dua.dalve.analyse.MqAnalyseModul;
import de.bsvrz.dua.dalve.prognose.PrognoseModul;
import de.bsvrz.dua.dalve.stoerfall.StoerfallModul;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.adapter.AbstraktVerwaltungsAdapterMitGuete;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.typen.SWETyp;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.DuaVerkehrsNetz;
import de.bsvrz.sys.funclib.bitctrl.modell.ObjektFactory;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;
import de.bsvrz.sys.funclib.debug.Debug;

// TODO: Auto-generated Javadoc
/**
 * Verwaltungsmodul der SWE Datenaufbereitung LVE. Hier werden nur die zu betrachtenden
 * Systemobjekte (alle Fahrstreifen in den übergebenen Konfigurationsbereichen) ermittelt, die
 * Datenanmeldung durchgeführt und die emfangenen Daten dann an das Analysemodul für Fahrstreifen
 * weitergereicht
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 * @version $Id$
 */
public class DatenaufbereitungLVE extends AbstraktVerwaltungsAdapterMitGuete {

	/**
	 * Attributgruppe unter der die geglaetteten Werte publiziert werden (FS).
	 */
	private static AttributeGroup pubAtgGlattFS = null;

	/**
	 * Attributgruppe unter der die Prognosewerte publiziert werden (FS).
	 */
	private static AttributeGroup pubAtgPrognoseFS = null;

	/**
	 * Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen (FS).
	 */
	private static AttributeGroup analyseAtgFS = null;

	/**
	 * Attributgruppe unter der die geglaetteten Werte publiziert werden (FS).
	 */
	private static AttributeGroup pubAtgGlattMQ = null;

	/**
	 * Attributgruppe unter der die Prognosewerte publiziert werden (FS).
	 */
	private static AttributeGroup pubAtgPrognoseMQ = null;

	/**
	 * Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen (FS).
	 */
	private static AttributeGroup analyseAtgMQ = null;

	/**
	 * Statische Datenverteiler-Verbindung.
	 */
	private static ClientDavInterface dDav = null;

	/** The instance. */
	private static DatenaufbereitungLVE instance = new DatenaufbereitungLVE();

	/** Konstruktor. */
	private DatenaufbereitungLVE() {
		// privater Konstruktor verhindert das Anlegen zusätzlicher Objekte der
		// Applikation.
	}

	/**
	 * Erfragt die Attributgruppe in der die Analysedaten dieses Objektes stehen.
	 *
	 * @param objekt
	 *            ein Systemobjekt.
	 * @return Attributgruppe in der die Analysedaten dieses Objektes stehen.
	 */
	public static AttributeGroup getAnalyseAtg(final SystemObject objekt) {
		if (objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
			return analyseAtgFS;
		} else if (objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			return analyseAtgMQ;
		} else {
			throw new RuntimeException("Fuer Objekt " + objekt + " vom Typ " + objekt.getType()
					+ " existiert keine Attributgruppe fuer Analysedaten.");
		}
	}

	/**
	 * Gets the single instance of DatenaufbereitungLVE.
	 *
	 * @return single instance of DatenaufbereitungLVE
	 */
	public static DatenaufbereitungLVE getInstance() {
		return instance;
	}

	/**
	 * Erfragt die Attributgruppe unter der die geglaetteten Werte publiziert werden.
	 *
	 * @param objekt
	 *            ein Systemobjekt.
	 * @return Attributgruppe unter der die geglaetteten Werte publiziert werden.
	 */
	public static AttributeGroup getPubAtgGlatt(final SystemObject objekt) {
		if (objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
			return pubAtgGlattFS;
		} else if (objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			return pubAtgGlattMQ;
		} else {
			throw new RuntimeException("Fuer Objekt " + objekt + " vom Typ " + objekt.getType()
					+ " existiert keine Attributgruppe fuer geglaettete Werte.");
		}
	}

	/**
	 * Erfragt die Attributgruppe unter der die Prognosewerte publiziert werden.
	 *
	 * @param objekt
	 *            ein Systemobjekt.
	 * @return Attributgruppe unter der die Prognosewerte publiziert werden.
	 */
	public static AttributeGroup getPubAtgPrognose(final SystemObject objekt) {
		if (objekt.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)) {
			return pubAtgPrognoseFS;
		} else if (objekt.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			return pubAtgPrognoseMQ;
		} else {
			throw new RuntimeException("Fuer Objekt " + objekt + " vom Typ " + objekt.getType()
					+ " existiert keine Attributgruppe fuer Prognosewerte.");
		}
	}

	/**
	 * Erfragt das Straßenteilsegment des Messquerschnitts.
	 *
	 * @param mq
	 *            the mq
	 * @return das Straßenteilsegment des Messquerschnitts oder <code>null</code>, wenn dieses nicht
	 *         ermittelbar ist.
	 */
	public static SystemObject getStraßenTeilSegment(final SystemObject mq) {
		SystemObject stsGesucht = null;

		if (mq.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			final Data mqData = mq.getConfigurationData(dDav.getDataModel().getAttributeGroup(
					"atg.punktLiegtAufLinienObjekt"));
			if (mqData != null) {
				if (mqData.getReferenceValue("LinienReferenz") != null) {
					final SystemObject strassenSegment = mqData.getReferenceValue("LinienReferenz")
							.getSystemObject();
					final double offset = mqData.getUnscaledValue("Offset").longValue() >= 0 ? mqData
							.getScaledValue("Offset").doubleValue() : -1.0;
									if ((strassenSegment != null) && strassenSegment.isOfType("typ.straßenSegment")
											&& (offset >= 0)) {
										final Data ssData = strassenSegment.getConfigurationData(dDav
												.getDataModel().getAttributeGroup("atg.bestehtAusLinienObjekten"));
										if (ssData != null) {
											double gesamtLaenge = 0;
											for (int i = 0; i < ssData.getArray("LinienReferenz").getLength(); i++) {
												if (ssData.getReferenceArray("LinienReferenz").getReferenceValue(i) != null) {
													final SystemObject sts = ssData
															.getReferenceArray("LinienReferenz")
															.getReferenceValue(i).getSystemObject();
													if ((sts != null) && sts.isOfType("typ.straßenTeilSegment")) {
														final Data stsData = sts.getConfigurationData(dDav
												.getDataModel().getAttributeGroup("atg.linie"));
														if (stsData != null) {
															final double laenge = stsData.getUnscaledValue("Länge")
																	.longValue() >= 0 ? stsData.getScaledValue(
													"Länge").doubleValue() : -1.0;
																			if (laenge >= 0) {
																				gesamtLaenge += laenge;
																			}
														}
														if (gesamtLaenge >= offset) {
															stsGesucht = sts;
															break;
														}
													}
												}
											}
										}
									}
				}
			}
		}

		return stsGesucht;
	}

	/**
	 * Startet diese Applikation.
	 *
	 * @param argumente
	 *            Argumente der Kommandozeile
	 */
	public static void main(final String argumente[]) {
		StandardApplicationRunner.run(new DatenaufbereitungLVE(), argumente);
	}

	/** Modul in dem die Fahrstreifen-Daten analysiert werden. */
	private FsAnalyseModul fsAnalyseModul = null;

	/** The ignore dichte max. */
	private boolean ignoreDichteMax;

	/**
	 * {@inheritDoc}.<br>
	 *
	 * Standard-Gütefaktor für Ersetzungen (90%)<br>
	 * Wenn das Modul Datenaufbereitung LVE einen Messwert ersetzt so vermindert sich die Güte des
	 * Ausgangswertes um diesen Faktor (wenn kein anderer Wert über die Kommandozeile übergeben
	 * wurde)
	 */
	@Override
	public double getStandardGueteFaktor() {
		return 0.9;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SWETyp getSWETyp() {
		return SWETyp.SWE_DATENAUFBEREITUNG_LVE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialisiere() throws DUAInitialisierungsException {
		super.initialisiere();

		final ArgumentList argumentList = new ArgumentList(
				komArgumente.toArray(new String[komArgumente.size()]));
		ignoreDichteMax = argumentList.fetchArgument("-ignoreDichteMax=false").booleanValue();

		ObjektFactory.getInstanz().setVerbindung(getVerbindung());

		dDav = getVerbindung();
		pubAtgPrognoseFS = dDav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_TRENT_FS);
		pubAtgGlattFS = dDav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_FS);
		analyseAtgFS = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS);
		pubAtgPrognoseMQ = dDav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_TRENT_MQ);
		pubAtgGlattMQ = dDav.getDataModel().getAttributeGroup(
				DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_MQ);
		analyseAtgMQ = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ);

		/**
		 * Initialisiere das DUA-Verkehrsnetz
		 */
		DuaVerkehrsNetz.initialisiere(verbindung);

		/**
		 * Ermittle nur die Fahrstreifen, Messquerschnitte und Straßenabschnitte
		 */
		final Collection<SystemObject> fahrStreifen = DUAUtensilien.getBasisInstanzen(verbindung
				.getDataModel().getType(DUAKonstanten.TYP_FAHRSTREIFEN), verbindung,
				getKonfigurationsBereiche());
		final Collection<SystemObject> messQuerschnitte = DUAUtensilien.getBasisInstanzen(
				verbindung.getDataModel().getType(DUAKonstanten.TYP_MQ_ALLGEMEIN), verbindung,
				getKonfigurationsBereiche());
		final Collection<SystemObject> abschnitte = DUAUtensilien.getBasisInstanzen(verbindung
				.getDataModel().getType(DUAKonstanten.TYP_STRASSEN_ABSCHNITT), verbindung,
						getKonfigurationsBereiche());
		objekte = fahrStreifen.toArray(new SystemObject[0]);

		final Collection<SystemObject> lveObjekte = new HashSet<SystemObject>();
		lveObjekte.addAll(fahrStreifen);
		lveObjekte.addAll(messQuerschnitte);

		String infoStr = Constants.EMPTY_STRING;
		for (final SystemObject obj : objekte) {
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		Debug.getLogger().config("---\nBetrachtete Fahrstreifen:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$

		infoStr = Constants.EMPTY_STRING;
		for (final SystemObject obj : messQuerschnitte) {
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		Debug.getLogger().config("---\nBetrachtete Messquerschnitte:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$

		fsAnalyseModul = new FsAnalyseModul();
		fsAnalyseModul.setPublikation(true);
		fsAnalyseModul.initialisiere(this);

		new MqAnalyseModul().initialisiere(this);

		new PrognoseModul().initialisiere(verbindung, lveObjekte);

		final Collection<SystemObject> stoerfallObjekte = new HashSet<SystemObject>();
		stoerfallObjekte.addAll(lveObjekte);
		stoerfallObjekte.addAll(abschnitte);
		new StoerfallModul().initialisiere(verbindung, stoerfallObjekte);

		verbindung.subscribeReceiver(
				this,
				objekte,
				new DataDescription(verbindung.getDataModel().getAttributeGroup(
						DUAKonstanten.ATG_KZD), verbindung.getDataModel().getAspect(
						DUAKonstanten.ASP_MESSWERTERSETZUNG)), ReceiveOptions.normal(),
				ReceiverRole.receiver());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final ResultData[] resultate) {
		fsAnalyseModul.aktualisiereDaten(resultate);
	}

	/**
	 * ermittelt, ob die Maximalwerte für die Dichteberechnung ignoriert werden sollen.
	 *
	 * @return den Zustand
	 */
	public boolean isIgnoreDichteMax() {
		return ignoreDichteMax;
	}

}
