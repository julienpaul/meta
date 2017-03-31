import 'whatwg-fetch';
import {getJson, checkStatus} from 'icos-cp-backend';
import {copyprops} from 'icos-cp-utils';

export function listClasses(){
	return fetch('/edit/cpmeta/getExposedClasses')
		.then(checkStatus)
		.then(types => types.json());
}

