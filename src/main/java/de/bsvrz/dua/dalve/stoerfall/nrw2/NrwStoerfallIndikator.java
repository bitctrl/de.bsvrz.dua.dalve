/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
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
package de.bsvrz.dua.dalve.stoerfall.nrw2;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dua.dalve.stoerfall.KVStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;

/**
 * Repräsentiert einen Stoerfallindikator nach Verfahren NRW (nur fuer
 * Fahrstreifen)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 */
public class NrwStoerfallIndikator extends KVStoerfallIndikator {

	/**
	 * letzter errechneter Störfallzustand
	 */
	protected StoerfallSituation letzteStufe = StoerfallSituation.KEINE_AUSSAGE;

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

	@Override
	protected void berechneIndikator(final long timeStamp, long v, long q) {
		final Data data;
		StoerfallSituation situation = StoerfallSituation.KEINE_AUSSAGE;

		long k;
		
		if(v == DUAKonstanten.NICHT_ERMITTELBAR){
			v = 0;
			k = 0;
		}
		else {
			k = Math.round((double)q / v);
		}

		if (v >= 0 && k >= 0 && v1 >= 0 && v2 >= 0 && k1 >= 0 && k2 >= 0 && k3 >= 0 && kT >= 0) {
			situation = getVerkehrsStufe(k, v);
		}

		StoerfallZustand zustand = new StoerfallZustand(DAV);
		zustand.setT(getT());
		zustand.setSituation(situation);
		data = zustand.getData();
		letzteStufe = situation;

		ResultData ergebnis = new ResultData(this.objekt, this.pubBeschreibung, timeStamp, data);
		this.sendeErgebnis(ergebnis);
	}

	protected StoerfallSituation getVerkehrsStufe(final double k, final double v) {
		final StoerfallSituation situation;
		if( k == 0 && v == 0){
			situation = letzteStufe;
		}
		else if(k > k3 || (v < v1 && letzteStufe.getCode() >= 4)){
			situation = StoerfallSituation.STAU;
		}
		else if(v < v2 && (k > kT || letzteStufe.getCode() >= 4)){
			situation = StoerfallSituation.ZAEHER_VERKEHR;
		}
		else if(k > k2 && v >= v2){
			situation = StoerfallSituation.DICHTER_VERKEHR;
		}
		else if(k > k1 && v >= v2){
			situation = StoerfallSituation.LEBHAFTER_VERKEHR;
		}
		else if(k >= 0 && v >= v2){
			situation = StoerfallSituation.FREIER_VERKEHR;
		}
		else {
			situation = letzteStufe;
		}
		return situation;
	}

}
