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
package de.bsvrz.dua.dalve;

import java.util.Collection;

import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ReceiveOptions;
import stauma.dav.clientside.ReceiverRole;
import stauma.dav.clientside.ResultData;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.dua.dalve.fs.FsAnalyseModul;
import de.bsvrz.dua.dalve.mq.MqAnalyseModul;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.adapter.AbstraktVerwaltungsAdapterMitGuete;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.typen.SWETyp;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.DuaVerkehrsNetz;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Verwaltungsmodul der SWE Datenaufbereitung LVE. Hier werden nur die 
 * zu betrachtenden Systemobjekte (alle Fahrstreifen in den übergebenen
 * Konfigurationsbereichen) ermittelt, die Datenanmeldung durchgeführt 
 * und die emfangenen Daten dann an das Analysemodul für Fahrstreifen
 * weitergereicht
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DatenaufbereitungLVE
extends AbstraktVerwaltungsAdapterMitGuete{
	
	/**
	 * Modul in dem die Fahrstreifen-Daten analysiert werden
	 */
	private FsAnalyseModul fsAnalyseModul = null;	
	
	/**
	 * Modul in dem die MQ-Daten analysiert werden
	 */
	private MqAnalyseModul mqAnalyseModul = null;
	
	
	/**
	 * {@inheritDoc}
	 */
	public SWETyp getSWETyp() {
		return SWETyp.SWE_DATENAUFBEREITUNG_LVE;
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialisiere()
	throws DUAInitialisierungsException {
		
		DuaVerkehrsNetz.initialisiere(this.verbindung);
		
		/**
		 * Ermittle nur die Fahrstreifen
		 */
		String infoStr = Konstante.LEERSTRING;
		Collection<SystemObject> daLveObjekte = DUAUtensilien.getBasisInstanzen(
				this.verbindung.getDataModel().getType(DUAKonstanten.TYP_FAHRSTREIFEN),
				this.verbindung, this.getKonfigurationsBereiche());
		this.objekte = daLveObjekte.toArray(new SystemObject[0]);
		
		for(SystemObject obj:this.objekte){
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		LOGGER.config("---\nBetrachtete Objekte:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$
		
		this.fsAnalyseModul = new FsAnalyseModul();
		this.fsAnalyseModul.setPublikation(true);
		this.fsAnalyseModul.initialisiere(this);
				
		this.mqAnalyseModul = new MqAnalyseModul();
		this.mqAnalyseModul.initialisiere(this);
		
		DataDescription anmeldungsBeschreibungKZD = new DataDescription(
				this.verbindung.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD),
				this.verbindung.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG),
				(short)0);
			
		this.verbindung.subscribeReceiver(this, this.objekte, anmeldungsBeschreibungKZD,
					ReceiveOptions.normal(), ReceiverRole.receiver());
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public void update(ResultData[] resultate) {
		this.fsAnalyseModul.aktualisiereDaten(resultate);
	}
	
	
	/**
	 * Startet diese Applikation
	 * 
	 * @param args Argumente der Kommandozeile
	 */
	public static void main(String argumente[]){
        Thread.setDefaultUncaughtExceptionHandler(new Thread.
        				UncaughtExceptionHandler(){
            public void uncaughtException(@SuppressWarnings("unused")
			Thread t, Throwable e) {
                LOGGER.error("Applikation wird wegen" +  //$NON-NLS-1$
                		" unerwartetem Fehler beendet", e);  //$NON-NLS-1$
                e.printStackTrace();
                Runtime.getRuntime().exit(0);
            }
        });
		StandardApplicationRunner.run(
					new DatenaufbereitungLVE(), argumente);
	}

	
	/**
	 * {@inheritDoc}.<br>
	 * 
	 * Standard-Gütefaktor für Ersetzungen (90%)<br>
	 * Wenn das Modul Datenaufbereitung LVE einen Messwert ersetzt so vermindert sich
	 * die Güte des Ausgangswertes um diesen Faktor (wenn kein anderer Wert über die
	 * Kommandozeile übergeben wurde)
	 */
	@Override
	public double getStandardGueteFaktor() {
		return 0.9;
	}

}
