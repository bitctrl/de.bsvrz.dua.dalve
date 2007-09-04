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

import stauma.dav.clientside.ClientDavInterface;
import stauma.dav.clientside.Data;
import stauma.dav.configuration.interfaces.SystemObject;
import de.bsvrz.dua.dalve.prognose.PrognoseSystemObjekt;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.FahrStreifen;
import de.bsvrz.sys.funclib.bitctrl.dua.lve.MessQuerschnitt;

/**
 * Objekt, das mit einem Systemobjekt assoziiert ist, welches innerhalb der 
 * Stoerfallanalyse behandelt werden kann (also <code>typ.fahrStreifen</code>
 * oder <code>typ.messQuerschnitt</code>)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class StoerfallSystemObjekt
extends PrognoseSystemObjekt{

	/**
	 * Infrastrukturobjekt, mit dem dieses Stoerfallindikator-Objekt assoziiert ist
	 */
	private SystemObject infrastrukturObjekt = null;
	
	
	/**
	 * Standardkonstruktor<br>
	 * <b>Achtung:</b> Es wird hier davon ausgegangen, dass das DUA-Verkehrsnetz bereits
	 * initialisiert wurde
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekt ein Systemobjekt, welches innerhalb der Stoerfallanalyse
	 * behandelt werden kann (also <code>typ.fahrStreifen</code> oder
	 * <code>typ.messQuerschnitt</code>)
	 * @throws DUAInitialisierungsException wenn das Objekt nicht vollständig initialisiert
	 * werden konnte
	 */
	public StoerfallSystemObjekt(ClientDavInterface dav,
								 SystemObject objekt)
	throws DUAInitialisierungsException {
		super(dav, objekt);
		
		SystemObject mqAmInfraStrukturObjekt = null;
		
		if(this.isFahrStreifen()){
			FahrStreifen fs = FahrStreifen.getInstanz(objekt);
			
			if(fs == null){
				throw new DUAInitialisierungsException("Konfiguration von Fahrstreifen " + objekt  //$NON-NLS-1$
						+ " konnte nicht ausgelesen werden");  //$NON-NLS-1$
			}
			
			for(MessQuerschnitt mq:MessQuerschnitt.getInstanzen()){
				if(mq.getFahrStreifen().contains(fs)){
					mqAmInfraStrukturObjekt = mq.getSystemObject();
					break;
				}
			}
		}else{
			mqAmInfraStrukturObjekt = objekt;
		}
		
		if(mqAmInfraStrukturObjekt != null){
			Data data = mqAmInfraStrukturObjekt.getConfigurationData(
					dav.getDataModel().getAttributeGroup("atg.punktLiegtAufLinienObjekt"));  //$NON-NLS-1$
			if(data != null){
				if( data.getReferenceValue("LinienReferenz") != null ){  //$NON-NLS-1$
					this.infrastrukturObjekt = data.getReferenceValue("LinienReferenz").getSystemObject();   //$NON-NLS-1$
				}
			}
		}
		
		if(this.infrastrukturObjekt == null){
			throw new DUAInitialisierungsException("Mit " + objekt + " assoziiertes " +  //$NON-NLS-1$  //$NON-NLS-2$
					"Infrastrukturobjekt konnte nicht bestimmt werden");  //$NON-NLS-1$
		}
	}


	/**
	 * Erfragt das Infrastrukturobjekt, mit dem dieses Stoerfallindikator-Objekt
	 * assoziiert ist
	 * 
	 * @return das Infrastrukturobjekt
	 */
	public final SystemObject getInfrastrukturObjekt() {
		return infrastrukturObjekt;
	}	
	
}
