/*
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Wei�enfelser Stra�e 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.analyse;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data.Array;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.AllgemeinerDatenContainer;

/**
 * Korrespondiert mit der Attributgruppe <code>atg.verkehrsDatenKurzZeitAnalyseMq</code>.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 * @version $Id$
 */
public class AtgVerkehrsDatenKurzZeitAnalyseMq extends AllgemeinerDatenContainer implements
ClientReceiverInterface {

	/**
	 * <code>KKfz.Grenz</code>.
	 */
	private long kKfzGrenz = -4;

	/**
	 * <code>KKfz.Max</code>.
	 */
	private long kKfzMax = -4;

	/**
	 * <code>KLkw.Grenz</code>.
	 */
	private long kLkwGrenz = -4;

	/**
	 * <code>KLkw.Max</code>.
	 */
	private long kLkwMax = -4;

	/**
	 * <code>KPkw.Grenz</code>.
	 */
	private long kPkwGrenz = -4;

	/**
	 * <code>KPkw.Max</code>.
	 */
	private long kPkwMax = -4;

	/**
	 * <code>KB.Grenz</code>.
	 */
	private long kBGrenz = -4;

	/**
	 * <code>KB.Max</code>.
	 */
	private long kBMax = -4;

	/**
	 * <code>fl.k1</code>.
	 */
	private double flk1 = -4;

	/**
	 * <code>fl.k2</code>.
	 */
	private double flk2 = -4;

	/**
	 * Wichtung zur Ermittlung der Differenzgeschwindigkeit im Messquerschnitt.
	 */
	private int[] wichtung = null;

	/**
	 * Standardkonstruktor.
	 *
	 * @param dav
	 *            Datenverteiler-Verbindung
	 * @param mq
	 *            ein Systemobjekt eines Messquerschnittes
	 */
	public AtgVerkehrsDatenKurzZeitAnalyseMq(final ClientDavInterface dav, final SystemObject mq) {
		if (dav == null) {
			throw new NullPointerException("Datenverteiler-Verbindung ist <<null>>"); //$NON-NLS-1$
		}
		if (mq == null) {
			throw new NullPointerException("Uebergebenes Systemobjekt ist <<null>>"); //$NON-NLS-1$
		}
		dav.subscribeReceiver(
				this,
				mq,
				new DataDescription(dav.getDataModel().getAttributeGroup(
						"atg.verkehrsDatenKurzZeitAnalyseMq"), //$NON-NLS-1$
						dav.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_SOLL)),
				ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	/**
	 * Erfragt <code>KKfz.Grenz</code>.
	 *
	 * @return <code>KKfz.Grenz</code>
	 */
	public final long getKKfzGrenz() {
		return kKfzGrenz;
	}

	/**
	 * Erfragt <code>KKfz.Max</code>.
	 *
	 * @return <code>KKfz.Max</code>
	 */
	public final long getKKfzMax() {
		return kKfzMax;
	}

	/**
	 * Erfragt <code>KLkw.Grenz</code>.
	 *
	 * @return <code>KLkw.Grenz</code>
	 */
	public final long getKLkwGrenz() {
		return kLkwGrenz;
	}

	/**
	 * Erfragt <code>KLkw.Max</code>.
	 *
	 * @return <code>KLkw.Max</code>
	 */
	public final long getKLkwMax() {
		return kLkwMax;
	}

	/**
	 * Erfragt <code>KPkw.Grenz</code>.
	 *
	 * @return <code>KPkw.Grenz</code>
	 */
	public final long getKPkwGrenz() {
		return kPkwGrenz;
	}

	/**
	 * Erfragt <code>KPkw.Max</code>.
	 *
	 * @return <code>KPkw.Max</code>
	 */
	public final long getKPkwMax() {
		return kPkwMax;
	}

	/**
	 * Erfragt <code>KB.Grenz</code>.
	 *
	 * @return <code>KB.Grenz</code>
	 */
	public final long getKBGrenz() {
		return kBGrenz;
	}

	/**
	 * Erfragt <code>KB.Max</code>.
	 *
	 * @return <code>KB.Max</code>
	 */
	public final long getKBMax() {
		return kBMax;
	}

	/**
	 * Erfragt <code>fl.k1</code>.
	 *
	 * @return <code>fl.k1</code>
	 */
	public final double getFlk1() {
		return flk1;
	}

	/**
	 * Erfragt <code>fl.k2</code>.
	 *
	 * @return <code>fl.k2</code>
	 */
	public final double getFlk2() {
		return flk2;
	}

	/**
	 * Erfragt die Gewichtungsfaktoren.<br>
	 * Der Gewichtungsfaktor w(j) wird bei der Ermittlung der gewichteten Differenzgeschwindigkeit
	 * VDelta(i) im Messquerschnitt i ben�tigt ref.Afo . Dabei wichtet der Faktor w(1) die
	 * Differenzgeschwindigkeit zwischen dem Hauptfahrstreifen und dem 1. �berholfahrstreifen, w(2)
	 * die Differenzgeschwindigkeit zwischen dem 1. �FS und dem 2.�FS usw.. Die Summe der
	 * Gewichtungsfaktoren muss eins sein. <br>
	 * �ber den Parameter wichtung wird der Gewichtungsfaktor ermittelt. Wenn das Array keine
	 * Elemente enth�lt wird gleich gewichtet. D.h. die Differenzgeschwindigkeiten zwischen zwei
	 * benachbarten Fahrstreifen gehen zu gleichen Teilen in die Ermittlung von VDelta ein.<br>
	 * Hinweis: Die Wichtung kann erst ab mindestens drei Fahrstreifen gesetzt werden. Wenn das
	 * Array Elemente enth�lt werden die Werte f�r die Wichtung der Differenzgeschwindigkeiten
	 * zwischen den benachbarten Fahrstreifen umgerechnet: <br>
	 * Wenn zu wenig Werte vorgegeben sind, werden die weiteren ben�tigten Werte durch duplizieren
	 * des letzten Wertes erzeugt. F�r einen Messquerschnitt mit 4 Fahrstreifen werden z.B. drei
	 * Werte ben�tigt. Ist als wichtung (60,40) parametriert, wird f�r die Ermittlung der
	 * Gewichtungsfaktoren das Array (60,40,40) betrachtet. Wenn die Summe der Werte 100 Prozent
	 * �berschreitet oder unterschreitet werden die Gewichtungswerte auf 100 Prozent normiert. Damit
	 * ergeben sich aus dem obigen Beispiel folgende Wichtungswerte: w(1)=60/140, w(2)=w(3)=40/140.
	 *
	 * @return die Gewichtungsfaktoren
	 */
	public final int[] getWichtung() {
		return wichtung == null ? new int[0] : wichtung;
	}

	/**
	 * Erfragt, ob dieses Objekt bereits Parameter emfangen hat.
	 *
	 * @return ob dieses Objekt bereits Parameter emfangen hat
	 */
	public final boolean isInitialisiert() {
		return flk1 != -4;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(final ResultData[] resultate) {
		if (resultate != null) {
			for (final ResultData resultat : resultate) {
				if ((resultat != null) && (resultat.getData() != null)) {
					synchronized (this) {
						flk1 = resultat.getData().getItem("fl").getScaledValue("k1").doubleValue(); //$NON-NLS-1$ //$NON-NLS-2$
						flk2 = resultat.getData().getItem("fl").getScaledValue("k2").doubleValue(); //$NON-NLS-1$ //$NON-NLS-2$

						kBGrenz = resultat.getData()
								.getItem("KB").getUnscaledValue("Grenz").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
						kBMax = resultat.getData()
								.getItem("KB").getUnscaledValue("Max").longValue(); //$NON-NLS-1$ //$NON-NLS-2$

						kKfzGrenz = resultat.getData()
								.getItem("KKfz").getUnscaledValue("Grenz").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
						kKfzMax = resultat.getData()
								.getItem("KKfz").getUnscaledValue("Max").longValue(); //$NON-NLS-1$ //$NON-NLS-2$

						kLkwGrenz = resultat.getData()
								.getItem("KLkw").getUnscaledValue("Grenz").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
						kLkwMax = resultat.getData()
								.getItem("KLkw").getUnscaledValue("Max").longValue(); //$NON-NLS-1$ //$NON-NLS-2$

						kPkwGrenz = resultat.getData()
								.getItem("KPkw").getUnscaledValue("Grenz").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
						kPkwMax = resultat.getData()
								.getItem("KPkw").getUnscaledValue("Max").longValue(); //$NON-NLS-1$ //$NON-NLS-2$

						final Array array = resultat.getData().getArray("wichtung"); //$NON-NLS-1$
						if (array.getLength() > 0) {
							wichtung = new int[array.getLength()];
						}

						for (int i = 0; i < array.getLength(); i++) {
							wichtung[i] = array.getItem(i).asUnscaledValue().intValue();
						}
					}
				}
			}
		}
	}

}