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
package de.bsvrz.dua.dalve.mq;

import stauma.dav.clientside.Data;
import stauma.dav.clientside.ResultData;
import sys.funclib.debug.Debug;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;

/**
 * Instanzen dieser Klasse repräsentieren Q-Werte die entsprechend SE-02.00.00.00.00-AFo-4.0
 * (S.121) additiv und subtraktiv miteinander verknüpft werden können
 *  
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class QWert {
	
	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
	
	/**
	 * Der Wert
	 */
	private MesswertUnskaliert wert = null; 
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param resultat ein KZD-Resultat (kann auch <code>null</code> sein)
	 * @param attName der Name des Zielattributes (Q-Wert) innerhalb des KZD-Resultates 
	 */
	public QWert(ResultData resultat, String attName){
		if(resultat != null && resultat.getData() != null){
			this.wert = new MesswertUnskaliert(attName, resultat.getData());	
		}		
	}
		
	
	/**
	 * Interner Ergebniskonstruktor
	 * 
	 * @param attName der Name des Zielattributes (Q-Wert)
	 */
	private QWert(String attName){
		this.wert = new MesswertUnskaliert(attName);	
	}
	
	
	/**
	 * Erfragt, ob dieses Datum verrechenbar ist. Dies ist dann der Fall,
	 * wenn das Datum Nutzdaten enthält, die <code> >= 0</code> sind
	 * 
	 * @return ob dieses Datum verrechenbar ist
	 */
	public final boolean isVerrechenbar(){
		return this.wert != null && this.wert.getWertUnskaliert() >= 0;
	}
	
	
	/**
	 * Erfragt, ob der in diesem Objekt stehende Wert in das übergebene Datum exportierbar ist
	 * 
	 * @param datum ein KZD-Nutzdatum
	 * @return  ob der in diesem Objekt stehende Wert in das übergebene Datum exportierbar ist
	 */
	public final boolean isExportierbarNach(Data datum){
		boolean exportierbar = false;
		
		if(datum != null && this.wert != null){
			exportierbar = DUAUtensilien.isWertInWerteBereich(datum.getItem(this.wert.getName()).getItem("Wert"), //$NON-NLS-1$
							this.wert.getWertUnskaliert());
		}			
			
		return exportierbar;
	}

	
	/**
	 * Erfragt den aktuellen Zustand des Messwertes, der von diesem Objekt 
	 * repräsentiert wird
	 * 
	 * @return der aktuelle Zustand des Messwertes, der von diesem Objekt 
	 * repräsentiert wird
	 */
	public final MesswertUnskaliert getWert(){
		return this.wert;
	}
	
	
	/**
	 * Berechnet die Summe der beiden übergebenen Werte entsprechend
	 * SE-02.00.00.00.00-AFo-4.0 (S.121)
	 * 
	 * @param summand1 1. Summand (kann auch <code>null</code> sein)
	 * @param summand2 2. Summand (kann auch <code>null</code> sein)
	 * @return die Summe der beiden Summanden oder <code>null</code>, wenn
	 * diese nicht ermittelt werden konnte
	 */
	public static final QWert summe(QWert summand1, QWert summand2){
		QWert ergebnis = null;
		
		if(summand1 != null && summand2 != null){
			if(summand1.isVerrechenbar() && summand2.isVerrechenbar()){
				ergebnis = new QWert(summand1.getWert().getName());
				ergebnis.getWert().setWertUnskaliert(summand1.getWert().getWertUnskaliert() + 
													 summand2.getWert().getWertUnskaliert());
				ergebnis.getWert().setInterpoliert(summand1.getWert().isInterpoliert() || 
												   summand2.getWert().isInterpoliert());				
			}
			
			if( (summand1.isVerrechenbar() || 
				 summand1.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) &&
				(summand2.isVerrechenbar() ||
				 summand2.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)){
				/**
				 * Gueteberechnung
				 */
				try {
					GWert gueteSummand1 = new GWert(summand1.getWert().getGueteIndex(), 
													GueteVerfahren.getZustand(summand1.getWert().getVerfahren()),
													summand1.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR);
					GWert gueteSummand2 = new GWert(summand2.getWert().getGueteIndex(), 
													GueteVerfahren.getZustand(summand2.getWert().getVerfahren()),
													summand2.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR);
					GWert gueteGesamt = GueteVerfahren.summe(gueteSummand1, gueteSummand2);
					ergebnis.getWert().getGueteIndex().setWert(gueteGesamt.getIndexUnskaliert());					
					ergebnis.getWert().setVerfahren(gueteGesamt.getVerfahren().getCode());
				} catch (GueteException e) {
					LOGGER.error("Guete-Summe konnte nicht ermittelt werden.\n***Summand1***\n" + summand1.getWert() + //$NON-NLS-1$
							"\n***Summand2***\n" + summand2.getWert()); //$NON-NLS-1$
					e.printStackTrace();
				}
			}
		}
		
		return ergebnis;
	}

	
	/**
	 * Berechnet die Differenz der beiden übergebenen Werte entsprechend
	 * SE-02.00.00.00.00-AFo-4.0 (S.121)
	 * 
	 * @param minuend der Minuend
	 * @param subtrahend der Subtrahend
	 * @return die Differenz der beiden übergebenen Werte entsprechend
	 * SE-02.00.00.00.00-AFo-4.0 (S.121) oder <code>null</code>, wenn
	 * diese nicht ermittelt werden konnte
	 */
	public static final QWert differenz(QWert minuend, QWert subtrahend){
		QWert ergebnis = null;
		
		if(minuend != null && subtrahend != null){
			if(minuend.isVerrechenbar() && subtrahend.isVerrechenbar()){
				ergebnis = new QWert(minuend.getWert().getName());
				ergebnis.getWert().setWertUnskaliert(minuend.getWert().getWertUnskaliert() - 
													 subtrahend.getWert().getWertUnskaliert());
				ergebnis.getWert().setInterpoliert(minuend.getWert().isInterpoliert() || 
												   subtrahend.getWert().isInterpoliert());				
			}
			
			if( (minuend.isVerrechenbar() || 
				 minuend.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) &&
				(subtrahend.isVerrechenbar() ||
				 subtrahend.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR)){
				/**
				 * Gueteberechnung
				 */
				try {
					GWert gueteMinuend = new GWert(minuend.getWert().getGueteIndex(), 
												   GueteVerfahren.getZustand(minuend.getWert().getVerfahren()), 
												   minuend.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR);
					GWert gueteSubtrahend = new GWert(subtrahend.getWert().getGueteIndex(), 
													  GueteVerfahren.getZustand(subtrahend.getWert().getVerfahren()),
													  subtrahend.getWert().getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR);
					GWert gueteGesamt = GueteVerfahren.differenz(gueteMinuend, gueteSubtrahend);
					ergebnis.getWert().getGueteIndex().setWert(gueteGesamt.getIndexUnskaliert());					
					ergebnis.getWert().setVerfahren(gueteGesamt.getVerfahren().getCode());
				} catch (GueteException e) {
					LOGGER.error("Guete-Differenz konnte nicht ermittelt werden.\n***Minuend***\n" + minuend.getWert() + //$NON-NLS-1$
							"\n***Subrahend***\n" + subtrahend.getWert()); //$NON-NLS-1$
					e.printStackTrace();
				}				
			}
		}
		
		return ergebnis;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.wert == null?"<<null>>":this.wert.toString(); //$NON-NLS-1$
	}
	
}
