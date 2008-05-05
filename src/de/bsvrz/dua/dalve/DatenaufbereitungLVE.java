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
import java.util.HashSet;

import com.bitctrl.Constants;

import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
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
import de.bsvrz.sys.funclib.debug.Debug;

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
		super.initialisiere();
		
		/**
		 * Initialisiere das DUA-Verkehrsnetz
		 */
		DuaVerkehrsNetz.initialisiere(this.verbindung, this.getKonfigurationsBereiche().toArray(new ConfigurationArea[0]));
		
		/**
		 * Ermittle nur die Fahrstreifen und Messquerschnitte
		 */
		Collection<SystemObject> fahrStreifen = DUAUtensilien.getBasisInstanzen(
				this.verbindung.getDataModel().getType(DUAKonstanten.TYP_FAHRSTREIFEN),
				this.verbindung,
				this.getKonfigurationsBereiche());
		Collection<SystemObject> messQuerschnitte = DUAUtensilien.getBasisInstanzen(
				this.verbindung.getDataModel().getType(DUAKonstanten.TYP_MQ_ALLGEMEIN),
				this.verbindung,
				this.getKonfigurationsBereiche());
		this.objekte = fahrStreifen.toArray(new SystemObject[0]);

		Collection<SystemObject> alleObjekte = new HashSet<SystemObject>();
		alleObjekte.addAll(fahrStreifen);
		alleObjekte.addAll(messQuerschnitte);
		
		String infoStr = Constants.EMPTY_STRING;
		for(SystemObject obj:objekte){
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		Debug.getLogger().config("---\nBetrachtete Fahrstreifen:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$

		infoStr = Constants.EMPTY_STRING;
		for(SystemObject obj:messQuerschnitte){
			infoStr += obj + "\n"; //$NON-NLS-1$
		}
		Debug.getLogger().config("---\nBetrachtete Messquerschnitte:\n" + infoStr + "---\n"); //$NON-NLS-1$ //$NON-NLS-2$

		this.fsAnalyseModul = new FsAnalyseModul();
		this.fsAnalyseModul.setPublikation(true);
		this.fsAnalyseModul.initialisiere(this);
				
		new MqAnalyseModul().initialisiere(this);
		
		new PrognoseModul().initialisiere(this.verbindung, alleObjekte);
		new StoerfallModul().initialisiere(this.verbindung, alleObjekte);
		
		this.verbindung.subscribeReceiver(this,
				this.objekte,
				new DataDescription(
						this.verbindung.getDataModel().getAttributeGroup(DUAKonstanten.ATG_KZD),
						this.verbindung.getDataModel().getAspect(DUAKonstanten.ASP_MESSWERTERSETZUNG),
						(short)0),
				ReceiveOptions.normal(),
				ReceiverRole.receiver());
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
	 * @param argumente Argumente der Kommandozeile
	 */
	public static void main(String argumente[]){
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
