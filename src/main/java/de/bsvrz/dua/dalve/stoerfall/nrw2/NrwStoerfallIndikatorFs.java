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
package de.bsvrz.dua.dalve.stoerfall.nrw2;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.DatenaufbereitungLVE;
import de.bsvrz.dua.dalve.prognose.PrognoseTyp;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;
import de.bsvrz.sys.funclib.debug.Debug;

// TODO: Auto-generated Javadoc
/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren NRW (nur fuer Fahrstreifen).
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class NrwStoerfallIndikatorFs extends AbstraktStoerfallIndikator {

	/** Grenzgeschwindigkeit 1 (0<v1<v2). */
	protected long v1 = -4;

	/** Grenzgeschwindigkeit 2 (0<v1<v2). */
	protected long v2 = -4;

	/** Grenzfahrzeugdichte 1 (0<k1<k2<K3 UND 0<kT<k3). */
	protected long k1 = -4;

	/** Grenzfahrzeugdichte 2 (0<k1<k2<K3 UND 0<kT<k3). */
	protected long k2 = -4;

	/** Grenzfahrzeugdichte 2 (0<k1<k2<K3 UND 0<kT<k3). */
	protected long k3 = -4;

	/** Grenzfahrzeugdichte 2 (0<k1<k2<K3 UND 0<kT<k3). */
	protected long kT = -4;

	/** letzter empfangener Analysedatensatz. */
	protected ResultData analyseDatensatz = null;

	/** letzter empfangener geglaetteter Datensatz. */
	protected ResultData geglaettetDatensatz = null;

	/** letzte ermittelte Verkehrsstufe. */
	protected StoerfallSituation letzteStufe = StoerfallSituation.KEINE_AUSSAGE;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(final ClientDavInterface dav, final SystemObject objekt)
			throws DUAInitialisierungsException {
		super.initialisiere(dav, objekt);

		/**
		 * Anmeldung auf Daten
		 */
		dav.subscribeReceiver(this, objekt,
				new DataDescription(DatenaufbereitungLVE.getPubAtgGlatt(this.objekt),
						PrognoseTyp.NORMAL.getAspekt()), ReceiveOptions.normal(), ReceiverRole
						.receiver());
		dav.subscribeReceiver(this, this.objekt,
				new DataDescription(DatenaufbereitungLVE.getAnalyseAtg(objekt), dav.getDataModel()
				.getAspect(DUAKonstanten.ASP_ANALYSE)), ReceiveOptions.normal(),
				ReceiverRole.receiver());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParameterAtgPid() {
		return "atg.verkehrsLageVerfahren2"; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.störfallVerfahrenNRW"; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void berechneStoerfallIndikator(final ResultData resultat) {
		Data data = null;

		if (resultat.getData() != null) {
			if (resultat.getDataDescription().getAttributeGroup()
					.equals(DatenaufbereitungLVE.getAnalyseAtg(objekt))) {
				analyseDatensatz = resultat;
			} else {
				geglaettetDatensatz = resultat;
			}

			if ((analyseDatensatz != null) && (geglaettetDatensatz != null)
					&& (analyseDatensatz.getDataTime() == geglaettetDatensatz.getDataTime())) {
				StoerfallSituation stufe = StoerfallSituation.KEINE_AUSSAGE;

				if ((v1 >= 0) && (v2 >= 0) && (k1 >= 0) && (k2 >= 0) && (k3 >= 0) && (kT >= 0)) {

					final long qKfzGNormal = geglaettetDatensatz.getData()
							.getItem("qKfzG").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
					final long vKfz = analyseDatensatz.getData()
							.getItem("vKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$

					if ((qKfzGNormal >= 0) && (vKfz > 0)) {
						final double kvst = (double) qKfzGNormal / (double) vKfz;
						double vvst = analyseDatensatz.getData()
								.getItem("vgKfz").getUnscaledValue("Wert").longValue(); //$NON-NLS-1$ //$NON-NLS-2$
						if (vvst < 0) {
							vvst = vKfz;
						}

						stufe = getVerkehrsStufe(kvst, vvst);
						letzteStufe = stufe;
					}
				}

				final StoerfallZustand zustand = new StoerfallZustand(DAV);
				zustand.setT(resultat.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$
				zustand.setSituation(stufe);
				data = zustand.getData();

				analyseDatensatz = null;
				geglaettetDatensatz = null;

				final ResultData ergebnis = new ResultData(objekt, pubBeschreibung,
						resultat.getDataTime(), data);
				sendeErgebnis(ergebnis);
			}

		} else {
			final ResultData ergebnis = new ResultData(objekt, pubBeschreibung,
					resultat.getDataTime(), data);
			sendeErgebnis(ergebnis);
		}
	}

	/**
	 * Errechnet die aktuelle Verkehrsstufe anhand der Parameter Verkehrsdichte und
	 * KFZ-Geschwindigkeit.
	 *
	 * @param kvst
	 *            Verkehrsdichte
	 * @param vvst
	 *            KFZ-Geschwindigkeit
	 * @return die aktuelle Verkehrsstufe
	 */
	protected StoerfallSituation getVerkehrsStufe(final double kvst, final double vvst) {
		StoerfallSituation verkehrsStufe = StoerfallSituation.KEINE_AUSSAGE;

		if (vvst <= v1) {
			if ((kvst > 0) && (kvst < k3)) {
				if (letzteStufe.equals(StoerfallSituation.KEINE_AUSSAGE)
						|| letzteStufe.equals(StoerfallSituation.STOERUNG)
						|| letzteStufe.equals(StoerfallSituation.ZAEHER_VERKEHR)
						|| letzteStufe.equals(StoerfallSituation.DICHTER_VERKEHR)) {
					verkehrsStufe = StoerfallSituation.STAU;
				} else {
					verkehrsStufe = letzteStufe;
				}
			} else if (kvst >= k3) {
				verkehrsStufe = StoerfallSituation.STAU;
			}
		} else if ((vvst > v1) && (vvst <= v2)) {
			if ((kvst > 0) && (kvst <= kT)) {
				if (letzteStufe.equals(StoerfallSituation.KEINE_AUSSAGE)
						|| letzteStufe.equals(StoerfallSituation.STAU)
						|| letzteStufe.equals(StoerfallSituation.STOERUNG)) {
					verkehrsStufe = StoerfallSituation.ZAEHER_VERKEHR;
				} else {
					verkehrsStufe = letzteStufe;
				}
			} else if ((kvst > kT) && (kvst <= k3)) {
				verkehrsStufe = StoerfallSituation.ZAEHER_VERKEHR;
			} else if (kvst > k3) {
				verkehrsStufe = StoerfallSituation.STAU;
			}
		} else if (vvst > v2) {
			if ((kvst > 0) && (kvst <= k1)) {
				verkehrsStufe = StoerfallSituation.FREIER_VERKEHR;
			} else if ((kvst > k1) && (kvst <= k2)) {
				verkehrsStufe = StoerfallSituation.LEBHAFTER_VERKEHR;
			} else if ((kvst > k2) && (kvst <= k3)) {
				verkehrsStufe = StoerfallSituation.DICHTER_VERKEHR;
			} else if (kvst > k3) {
				verkehrsStufe = StoerfallSituation.STAU;
			}
		}

		return verkehrsStufe;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readParameter(final ResultData parameter) {
		if (parameter.getData() != null) {
			v1 = parameter.getData().getUnscaledValue("v1").longValue(); //$NON-NLS-1$
			v2 = parameter.getData().getUnscaledValue("v2").longValue(); //$NON-NLS-1$
			k1 = parameter.getData().getUnscaledValue("k1").longValue(); //$NON-NLS-1$
			k2 = parameter.getData().getUnscaledValue("k2").longValue(); //$NON-NLS-1$
			k3 = parameter.getData().getUnscaledValue("k3").longValue(); //$NON-NLS-1$
			kT = parameter.getData().getUnscaledValue("kT").longValue(); //$NON-NLS-1$

			/**
			 * Konsitenz-Check
			 */
			if (!((v1 > 0) && (v1 < v2))) {
				Debug.getLogger().warning("Fehlerhafte Parameter (0 < v1 < v2) empfangen fuer " + //$NON-NLS-1$
						objekt + ": v1 = " + v1 + ", v2 = " + v2); //$NON-NLS-1$//$NON-NLS-2$
			}
			if (!((k1 > 0) && (k1 < k2) && (k2 < k3))) {
				Debug.getLogger().warning(
						"Fehlerhafte Parameter (0 < k1 < k2 < k3) empfangen fuer " + //$NON-NLS-1$
								objekt + ": k1 = " + k1 + ", k2 = " + k2 + ", k3 = " + k3); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			}
			if (!((kT > 0) && (kT < k3))) {
				Debug.getLogger().warning("Fehlerhafte Parameter (0 < kT < k3) empfangen fuer " + //$NON-NLS-1$
						objekt + ": kT = " + kT + ", k3 = " + k3); //$NON-NLS-1$//$NON-NLS-2$
			}
		} else {
			v1 = -4;
			v2 = -4;
			k1 = -4;
			k2 = -4;
			k3 = -4;
			kT = -4;
		}
	}
}
