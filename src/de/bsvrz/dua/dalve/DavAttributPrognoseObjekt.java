package de.bsvrz.dua.dalve;

import stauma.dav.clientside.Data;
import stauma.dav.clientside.ResultData;
import sys.funclib.debug.Debug;

public class DavAttributPrognoseObjekt
implements IAtgPrognoseParameterListener{

	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
	
	private PrognoseSystemObjekt prognoseObjekt = null;
	
	private PrognoseAttribut attribut = null;

	double alpha1 = Double.NaN;
	
	double alpha2 = Double.NaN;
	
	double beta1 = Double.NaN;
	
	double beta2 = Double.NaN;
		
	double ZAlt = Double.NaN;
	
	double deltaZAlt = 0.0;
	
	/**
	 * Prognosewert
	 */
	long ZP = -4;
	
	/**
	 * geglaetteter Wert ohne Prognoseanteil
	 */
	long ZG = -4;
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param prognoseObjekt
	 */
	public DavAttributPrognoseObjekt(PrognoseSystemObjekt prognoseObjekt){
		this.prognoseObjekt = prognoseObjekt;
	}
	
	public final void aktualisiere(final long ZAktuell){
		PrognoseAttributParameter parameter = null;/**parameterVerbindung.getParameterFuer(this.attribut);*/
		if(parameter != null){
			
			if(ZAktuell >= 0){
				if(ZAlt != Double.NaN){	// ZAlt wurde also bereits initialisiert (durch z.B. Parameter)
					double alpha = this.alpha1;
					double beta = this.beta1;
					if(ZAktuell > ZAlt){
						alpha = this.alpha2;
						beta = this.beta2;
					}
					
					double ZNeu = alpha * ZAktuell + (1.0 - alpha) * ZAlt;
					double deltaZNeu = beta * (ZAktuell - ZAlt) + (1 - beta) * deltaZAlt;
					this.ZP = Math.round(ZNeu + deltaZNeu);
					/**
					 * 1. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
					 */
					if(this.ZP < 0){
						this.ZP = 0;
					}
					/**
					 * 2. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
					 */
					else if(this.ZP == 0){
						
					}						
					this.ZG = Math.round(ZNeu);
					
					this.ZAlt = ZNeu;
					this.deltaZAlt = deltaZNeu;
				}else{
					LOGGER.warning("Fuer Attribut " + attribut.toString() + //$NON-NLS-1$
						" koennen keine Prognosewerte ermittelt werden. ZAlt wurde noch nicht initialisiert"); //$NON-NLS-1$
				}
			}else{
				// TODO
			}
		}
	}



	public final void aktualisiere(ResultData resultat)
	throws PrognoseParameterException{
		
	}
	
	
	public final void exportiereDatenGlatt(Data zielDatum){
		
	}
	
	public final void exportiereDatenPrognose(Data zielDatum){
		
	}


	/**
	 * {@inheritDoc}
	 */
	public void aktualisiereParameter(PrognoseAttributParameter parameterSatzFuerAttribut) {
		this.ZAlt = parameterSatzFuerAttribut.getStart();
		this.alpha1 = parameterSatzFuerAttribut.getAlpha1();
		this.alpha2 = parameterSatzFuerAttribut.getAlpha2();
		this.beta1 = parameterSatzFuerAttribut.getBeta1();
		this.beta2 = parameterSatzFuerAttribut.getBeta2();
	}

}
