/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * Copyright 2015 by Kappich Systemberatung Aachen
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dua.dalve.
 * 
 * de.bsvrz.dua.dalve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dua.dalve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dua.dalve.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */
package de.bsvrz.dua.dalve;

import de.bsvrz.dav.daf.main.*;
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
import de.bsvrz.sys.funclib.debug.Debug;
import de.bsvrz.sys.funclib.operatingMessage.MessageSender;

import java.util.Collection;
import java.util.HashSet;

/**
 * Verwaltungsmodul der SWE Datenaufbereitung LVE. Hier werden nur die zu
 * betrachtenden Systemobjekte (alle Fahrstreifen in den übergebenen
 * Konfigurationsbereichen) ermittelt, die Datenanmeldung durchgeführt und die
 * emfangenen Daten dann an das Analysemodul für Fahrstreifen weitergereicht
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
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
	 * Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen
	 * (FS).
	 */
	public static AttributeGroup analyseAtgFS = null;

	/**
	 * Attributgruppe unter der die geglaetteten Werte publiziert werden (FS).
	 */
	private static AttributeGroup pubAtgGlattMQ = null;

	/**
	 * Attributgruppe unter der die Prognosewerte publiziert werden (FS).
	 */
	private static AttributeGroup pubAtgPrognoseMQ = null;

	/**
	 * Attributgruppe aus der sich die Prognosedaten dieses Objektes speisen
	 * (FS).
	 */
	private static AttributeGroup analyseAtgMQ = null;

	/**
	 * Statische Datenverteiler-Verbindung.
	 */
	private static ClientDavInterface dDav = null;

	/**
	 * Modul in dem die Fahrstreifen-Daten analysiert werden
	 */
	private FsAnalyseModul fsAnalyseModul = null;

	public SWETyp getSWETyp() {
		return SWETyp.SWE_DATENAUFBEREITUNG_LVE;
	}

	@Override
	public void initialize(ClientDavInterface dieVerbindung) throws Exception {
		MessageSender.getInstance().setApplicationLabel("Datenaufbereitung LVE");
		super.initialize(dieVerbindung);
	}

	@Override
	protected void initialisiere() throws DUAInitialisierungsException {
		super.initialisiere();

		ObjektFactory.getInstanz().setVerbindung(this.getVerbindung());

		dDav = this.getVerbindung();
		pubAtgPrognoseFS = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_FS);
		pubAtgGlattFS = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_FS);
		analyseAtgFS = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS);
		pubAtgPrognoseMQ = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_TRENT_MQ);
		pubAtgGlattMQ = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_GEGLAETTET_MQ);
		analyseAtgMQ = dDav.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_MQ);

		/**
		 * Initialisiere das DUA-Verkehrsnetz
		 */
		DuaVerkehrsNetz.initialisiere(this.verbindung);

		/**
		 * Ermittle nur die Fahrstreifen, Messquerschnitte und Straßenabschnitte
		 */
		Collection<SystemObject> fahrStreifen = DUAUtensilien.getBasisInstanzen(
				this.verbindung.getDataModel().getType(DUAKonstanten.TYP_FAHRSTREIFEN), this.verbindung,
				this.getKonfigurationsBereiche());
		Collection<SystemObject> messQuerschnitte = DUAUtensilien.getBasisInstanzen(
				this.verbindung.getDataModel().getType(DUAKonstanten.TYP_MQ_ALLGEMEIN), this.verbindung,
				this.getKonfigurationsBereiche());
		Collection<SystemObject> abschnitte = DUAUtensilien.getBasisInstanzen(
				this.verbindung.getDataModel().getType(DUAKonstanten.TYP_STRASSEN_ABSCHNITT), this.verbindung,
				this.getKonfigurationsBereiche());
		this.objekte = fahrStreifen.toArray(new SystemObject[0]);

		Collection<SystemObject> lveObjekte = new HashSet<SystemObject>();
		lveObjekte.addAll(fahrStreifen);
		lveObjekte.addAll(messQuerschnitte);

		String infoStr = "";
		for (SystemObject obj : objekte) {
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		Debug.getLogger().config("---\nBetrachtete Fahrstreifen:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$

		infoStr = "";
		for (SystemObject obj : messQuerschnitte) {
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		Debug.getLogger().config("---\nBetrachtete Messquerschnitte:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$

		this.fsAnalyseModul = new FsAnalyseModul();
		this.fsAnalyseModul.setPublikation(true);
		this.fsAnalyseModul.initialisiere(this);

		new MqAnalyseModul().initialisiere(this);

		new PrognoseModul().initialisiere(this.verbindung, lveObjekte);

		Collection<SystemObject> stoerfallObjekte = new HashSet<SystemObject>();
		stoerfallObjekte.addAll(lveObjekte);
		stoerfallObjekte.addAll(abschnitte);
		new StoerfallModul().initialisiere(this.verbindung, stoerfallObjekte);

		this.verbindung.subscribeReceiver(this, this.objekte,
				new DataDescription(this.verbindung.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD),
						this.verbindung.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	public void update(ResultData[] resultate) {
		this.fsAnalyseModul.aktualisiereDaten(resultate);
	}

	/**
	 * Startet diese Applikation
	 * 
	 * @param argumente
	 *            Argumente der Kommandozeile
	 */
	public static void main(String argumente[]) {
		StandardApplicationRunner.run(new DatenaufbereitungLVE(), argumente);
	}

	/**
	 * Wenn das Modul Datenaufbereitung LVE einen Messwert ersetzt so vermindert
	 * sich die Güte des Ausgangswertes um diesen Faktor (wenn kein anderer Wert
	 * über die Kommandozeile übergeben wurde)
	 * 
	 * @return liefert den Standard-Gütefaktor für Ersetzungen (90%)
	 */
	@Override
	public double getStandardGueteFaktor() {
		return 0.9;
	}

	/**
	 * Erfragt die Attributgruppe in der die Analysedaten dieses Objektes
	 * stehen.
	 * 
	 * @param objekt
	 *            ein Systemobjekt.
	 * @return Attributgruppe in der die Analysedaten dieses Objektes stehen.
	 */
	public static AttributeGroup getAnalyseAtg(SystemObject objekt) {
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
	 * Erfragt die Attributgruppe unter der die geglaetteten Werte publiziert
	 * werden.
	 * 
	 * @param objekt
	 *            ein Systemobjekt.
	 * @return Attributgruppe unter der die geglaetteten Werte publiziert
	 *         werden.
	 */
	public static AttributeGroup getPubAtgGlatt(SystemObject objekt) {
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
	public static AttributeGroup getPubAtgPrognose(SystemObject objekt) {
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
	 *            der MQ für den ein Straßenteilsegment ermittelt werden soll
	 * 
	 * @return das Straßenteilsegment des Messquerschnitts oder
	 *         <code>null</code>, wenn dieses nicht ermittelbar ist.
	 */
	public static SystemObject getStrassenTeilSegment(SystemObject mq) {
		SystemObject stsGesucht = null;

		if (mq.isOfType(DUAKonstanten.TYP_MQ_ALLGEMEIN)) {
			Data mqData = mq
					.getConfigurationData(dDav.getDataModel().getAttributeGroup("atg.punktLiegtAufLinienObjekt"));
			if (mqData != null) {
				if (mqData.getReferenceValue("LinienReferenz") != null) {
					SystemObject linienReferenz = mqData.getReferenceValue("LinienReferenz").getSystemObject();
					if (linienReferenz != null) {
						if (linienReferenz.isOfType("typ.straßenTeilSegment")) {
							stsGesucht = linienReferenz;
						}
						if (linienReferenz.isOfType("typ.straßenSegment")) {
							double offset = mqData.getUnscaledValue("Offset").longValue() >= 0
									? mqData.getScaledValue("Offset").doubleValue() : -1.0;
							stsGesucht = getTeilSegmentWithOffset(linienReferenz, offset);
						}
					}
				}
			}
		}

		return stsGesucht;
	}

	private static SystemObject getTeilSegmentWithOffset(SystemObject segment, double offset) {

		SystemObject stsGesucht = null;

		Data ssData = segment
				.getConfigurationData(dDav.getDataModel().getAttributeGroup("atg.bestehtAusLinienObjekten"));
		if (ssData != null) {
			double gesamtLaenge = 0;
			for (int i = 0; i < ssData.getArray("LinienReferenz").getLength(); i++) {
				if (ssData.getReferenceArray("LinienReferenz").getReferenceValue(i) != null) {
					SystemObject sts = ssData.getReferenceArray("LinienReferenz").getReferenceValue(i)
							.getSystemObject();
					if (sts != null && sts.isOfType("typ.straßenTeilSegment")) {
						double laenge = getStsLaenge(sts);
						if (laenge >= 0) {
							gesamtLaenge += laenge;
						}
						if (gesamtLaenge >= offset) {
							stsGesucht = sts;
							break;
						}
					}
				}
			}
		}

		return stsGesucht;
	}

	private static double getStsLaenge(SystemObject sts) {
		Data linieData = sts.getConfigurationData(dDav.getDataModel().getAttributeGroup("atg.linie"));
		if (linieData != null) {
			if (linieData.getUnscaledValue("Länge").longValue() >= 0)
				return linieData.getScaledValue("Länge").doubleValue();
		}

		Data stsData = sts.getConfigurationData(dDav.getDataModel().getAttributeGroup("atg.straßenTeilSegment"));
		if (stsData != null) {
			if (stsData.getUnscaledValue("Länge").longValue() >= 0)
				return stsData.getScaledValue("Länge").doubleValue();
		}

		return -1.0;
	}

}
