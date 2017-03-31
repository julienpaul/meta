import {FETCHED_CLASSES} from './actions';

export default function(state, action){

	switch(action.type){

		case FETCHED_CLASSES:
			return update({types: action.types});

		default:
			return state;
	}

	function keep(props, updatesObj){
		return Object.assign(copyprops(state, props), updatesObj); 
	}

	function update(){
		const updates = Array.from(arguments);
		return Object.assign.apply(Object, [{}, state].concat(updates));
	}

	function updateWith(actionProps, path){
		return path
			? deepUpdate(state, path, copyprops(action, actionProps))
			: update(copyprops(action, actionProps));
	}

}
