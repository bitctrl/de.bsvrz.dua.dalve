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
package de.bsvrz.dua.dalve.stoerfall;

import java.util.Collection;
import java.util.HashSet;

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.dua.dalve.stoerfall.fd4.FdStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.marz1.MarzStoerfallIndikator;
import de.bsvrz.dua.dalve.stoerfall.nrw2.NrwStoerfallIndikatorFs;
import de.bsvrz.dua.dalve.stoerfall.nrw2.NrwStoerfallIndikatorMq;
import de.bsvrz.dua.dalve.stoerfall.rds3.RdsStoerfallIndikator;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;


/**
 * Von diesem Objekt aus wird die Berechnung der einzelnen
 * Stoerfallindikatoren gestartet
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class StoerfallModul {


	/**
	 * Initialisiert alle Stoerfallindikatoren
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekte Menge der Fahrstreifen und Messquerschnitte, die betrachtet werden sollen
	 * @throws DUAInitialisierungsException wird weitergereicht
	 */
	public final void initialisiere(final ClientDavInterface dav,
									final Collection<SystemObject> objekte) 
	throws DUAInitialisierungsException{

		Collection<StoerfallSystemObjekt> stoerfallObjekte = new HashSet<StoerfallSystemObjekt>();
		for(SystemObject obj:objekte){
			stoerfallObjekte.add(new StoerfallSystemObjekt(dav, obj));
		}
		
		/**
		 * (I) SFI nach Verfahren MARZ 
		 */
		for(StoerfallSystemObjekt obj:stoerfallObjekte){
			new MarzStoerfallIndikator().initialisiere(dav, obj);
		}
		

		/**
		 * (II) SFI nach Verfahren NRW  
		 */
		for(StoerfallSystemObjekt obj:stoerfallObjekte){
			if(obj.isFahrStreifen()){
				new NrwStoerfallIndikatorFs().initialisiere(dav, obj);
			}else{
				new NrwStoerfallIndikatorMq().initialisiere(dav, obj);
			}
		}
		

		/**
		 * (III) SFI nach Verfahren RDS  
		 */
		for(StoerfallSystemObjekt obj:stoerfallObjekte){
			if(!obj.isFahrStreifen()){
				new RdsStoerfallIndikator().initialisiere(dav, obj);
			}
		}

		
		/**
		 * (IV) SFI nach Verfahren Fundamentaldiagramm  
		 */
		for(StoerfallSystemObjekt obj:stoerfallObjekte){
			if(!obj.isFahrStreifen()){
				new FdStoerfallIndikator().initialisiere(dav, obj);				
			}
		}
	}
	
}
