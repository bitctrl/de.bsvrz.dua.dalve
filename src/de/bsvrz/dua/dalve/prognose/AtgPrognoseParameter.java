/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
package de.bsvrz.dua.dalve.prognose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.ClientReceiverInterface;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Haelt für ein bestimmtes Objekt (Fahrstreifen oder Messquerschnitt)
 * alle Parameter bereit die sich auf die Messwertprognose beziehen.
 * Dabei kann zwischen den Parametertypen <code>Flink</code>, <code>Normal</code>
 * und <code>Träge</code> unterschieden werden 
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AtgPrognoseParameter 
implements ClientReceiverInterface{

	/**
	 * das Objekt, auf dessen Prognose-Parameter
	 * sich angemeldet werden soll
	 */
	private PrognoseSystemObjekt objekt = null;
	
	/**
	 * Menge aktueller Werte der Attributparameter
	 */
	private Map<PrognoseAttribut, PrognoseAttributParameter> einzelWerte = 
									new HashMap<PrognoseAttribut, PrognoseAttributParameter>();
	
	/**
	 * Menge von Beobachtern einzelner Attributparameter
	 */
	private Map<PrognoseAttribut, Set<IAtgPrognoseParameterListener>> attributListener = 
									new HashMap<PrognoseAttribut, Set<IAtgPrognoseParameterListener>>();
	
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekt das Objekt, auf dessen Prognose-Parameter
	 * sich angemeldet werden soll
	 * @param typ der Typ der Parameter auf die sich angemeldet
	 * werden soll (Flink, Normal, Träge)
	 */
	public AtgPrognoseParameter(final ClientDavInterface dav,
								final PrognoseSystemObjekt objekt,
								final PrognoseTyp typ){
		this.objekt = objekt;
		this.initialisiere();
		dav.subscribeReceiver(this, 
							  objekt.getObjekt(), 
							  new DataDescription(
									  typ.getParameterAtg(objekt.isFahrStreifen()),
									  dav.getDataModel().getAspect(Konstante.DAV_ASP_PARAMETER_SOLL),
									  (short)0),
							  ReceiveOptions.normal(),
							  ReceiverRole.receiver());
	}
	
		
	/**
	 * Fügt der Menge aller Listener auf einen bestimmten Attributparameter 
	 * einen neuen Listener hinzu (und informiert diesen initial)
	 * 
	 * @param listener der neue Listener
	 * @param attribut das Attribut, auf dessen Parameter gehört werden soll
	 */
	public final void addListener(final IAtgPrognoseParameterListener listener,
								  final PrognoseAttribut attribut){
		Set<IAtgPrognoseParameterListener> beobachterMenge = this.attributListener.get(attribut);
		if(beobachterMenge == null){
			beobachterMenge = new HashSet<IAtgPrognoseParameterListener>();
			this.attributListener.put(attribut, beobachterMenge);
		}
		synchronized (this) {
			beobachterMenge.add(listener);
			listener.aktualisiereParameter(this.einzelWerte.get(attribut));			
		}
	}
	
	
	/**
	 * Informiert alle Beobachter über Veraenderungen 
	 */
	private final void informiereAlleBeobachter(){
		for(PrognoseAttribut attribut:this.attributListener.keySet()){
			PrognoseAttributParameter einzelWert = this.einzelWerte.get(attribut);
			for(IAtgPrognoseParameterListener listener:this.attributListener.get(attribut)){
				listener.aktualisiereParameter(einzelWert);
			}
		}
	}
	
	
	/**
	 * Initialisiert die Daten dieses Objekts auf den Zustand von
	 * <code>keine Daten</code>
	 */
	private final void initialisiere(){
		synchronized (this) {
			for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
				/**
				 * fuellt jeden Attributwert mit nicht initialisierten Werten
				 */
				this.einzelWerte.put(attribut, new PrognoseAttributParameter(attribut));
			}			
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					synchronized(this){
						if(resultat.getData() != null){
							for(PrognoseAttribut attribut:PrognoseAttribut.getInstanzen()){
								this.einzelWerte.get(attribut).setDaten(resultat.getData(), this.objekt.isFahrStreifen());
							}										
						}else{
							this.initialisiere();
						}
						this.informiereAlleBeobachter();
					}					
				}
			}
		}
	}	
}
