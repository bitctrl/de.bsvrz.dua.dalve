/**
 * Segment 4 Daten¸bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Weiﬂenfelser Straﬂe 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.stoerfall.marz1;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dua.dalve.stoerfall.AbstraktStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.StoerfallZustand;
import de.bsvrz.sys.funclib.bitctrl.modell.verkehr.zustaende.StoerfallSituation;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Repr‰sentiert einen Stoerfallindikator nach MARZ
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class MarzStoerfallIndikator
extends AbstraktStoerfallIndikator{
		
	/**
	 * MARZ-Situation <code>freier Verkehr</code>
	 */
	private static final StoerfallSituation Z1 = StoerfallSituation.FREIER_VERKEHR;

	/**
	 * MARZ-Situation <code>dichter Verkehr</code>
	 */
	private static final StoerfallSituation Z2 = StoerfallSituation.DICHTER_VERKEHR;

	/**
	 * MARZ-Situation <code>z‰hflieﬂender Verkehr</code>
	 */
	private static final StoerfallSituation Z3 = StoerfallSituation.ZAEHER_VERKEHR;

	/**
	 * MARZ-Situation <code>Stau</code>
	 */
	private static final StoerfallSituation Z4 = StoerfallSituation.STAU;
		
	/**
	 * Grenzgeschwindigkeit 1 (0<v1<v2) 
	 */
	private long v1 = -4; 
	
	/**
	 * Grenzgeschwindigkeit 2 (0<v1<v2)
	 */
	private long v2 = -4;
	
	/**
	 * Grenzfahrzeugdichte 2 (0<k1<k2)
	 */
	private long k1 = -4;
	
	/**
	 * Grenzfahrzeugdichte 2 (0<k1<k2)
	 */
	private long k2 = -4;
	
	/**
	 * letzter errechneter Stˆrfallzustand
	 */
	private StoerfallSituation letzterStoerfallZustand = StoerfallSituation.KEINE_AUSSAGE; 


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParameterAtgPid() {
		return "atg.verkehrsLageVerfahren1"; //$NON-NLS-1$
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPubAspektPid() {
		return "asp.stˆrfallVerfahrenMARZ"; //$NON-NLS-1$
	}
	
	
	/**
	 * Berechnet den aktuellen Stoerfallindikator anhand der empfangenen Daten
	 * analog MARZ 2004 (siehe 2.3.2.1.4 Verkehrssituationsuebersicht)
	 * 
	 * @param resultat ein empfangenes geglaettes Datum mit Nutzdaten
	 */
	protected void berechneStoerfallIndikator(ResultData resultat){
		Data data = null;
		
		if(resultat.getData() != null){			
			String attrV = this.objekt.isFahrStreifen()?"vKfzG":"VKfzG"; //$NON-NLS-1$ //$NON-NLS-2$
			String attrK = this.objekt.isFahrStreifen()?"kKfzG":"KKfzG"; //$NON-NLS-1$ //$NON-NLS-2$
			
			long v = resultat.getData().getItem(attrV).getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
			long k = resultat.getData().getItem(attrK).getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
			
			StoerfallSituation situation = StoerfallSituation.KEINE_AUSSAGE;
			
			if(v1 >= 0 && v2 >= 0 && k1 >= 0 && k2 >= 0){
				data = DAV.createData(this.pubBeschreibung.getAttributeGroup());
				
				if(v >= v2 && k >= 0 && k <= k1){
					situation = Z1;
				}
				if(v >= v2 && k > k1 && k <= k2){
					situation = Z2;
				}
				if(v >= v1 && v < v2 && k <= k2){
					situation = Z3;
				}
				if(v < v1 &&  k > k2){
					situation = Z4;
				}else
				if(v < v1 || k > k2){
					if(letzterStoerfallZustand.equals(Z3) || 
					   letzterStoerfallZustand.equals(Z4)){
						situation = Z4;
					}else{
						if(v >= v2 && k > k2){
							situation = Z2;
						}
						if( (v < v2 && k > k2) ||
							(v < v1 && k <= k2) ){
							situation = Z3;
						}
					}
				}
			}
			
			StoerfallZustand zustand = new StoerfallZustand(DAV);
			if(this.objekt.isFahrStreifen()){
				zustand.setT(resultat.getData().getTimeValue("T").getMillis()); //$NON-NLS-1$
			}
			zustand.setSituation(situation);
			data = zustand.getData();
		}
		
		ResultData ergebnis = new ResultData(this.objekt.getObjekt(), 
				this.pubBeschreibung, resultat.getDataTime(), data);
		this.sendeErgebnis(ergebnis);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readParameter(ResultData parameter) {
		if(parameter.getData() != null){
			this.v1 = parameter.getData().getUnscaledValue("v1").longValue(); //$NON-NLS-1$
			this.v2 = parameter.getData().getUnscaledValue("v2").longValue(); //$NON-NLS-1$
			this.k1 = parameter.getData().getUnscaledValue("k1").longValue(); //$NON-NLS-1$
			this.k2 = parameter.getData().getUnscaledValue("k2").longValue(); //$NON-NLS-1$
			/**
			 * Konsitenz-Check
			 */
			if( !(v1 > 0 && v1 < v2) ){
				Debug.getLogger().warning("Fehlerhafte Parameter (0<v1<v2) empfangen fuer " + //$NON-NLS-1$
						this.objekt + ": v1 = " + v1 + ", v2 = " + v2);  //$NON-NLS-1$//$NON-NLS-2$
			}
			if( !(k1 > 0 && k1 < k2) ){
				Debug.getLogger().warning("Fehlerhafte Parameter (0<k1<k2) empfangen fuer " + //$NON-NLS-1$
						this.objekt + ": k1 = " + k1 + ", k2 = " + k2);  //$NON-NLS-1$//$NON-NLS-2$
			}							
		}else{
			this.v1 = -4;
			this.v2 = -4;
			this.k1 = -4;
			this.k2 = -4;
		} 
	}
	
}
