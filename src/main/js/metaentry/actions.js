import {listClasses} from './backend';

export const ERROR = 'ERROR';
export const FETCHED_CLASSES = 'FETCHED_CLASSES';


function failWithError(error){
	console.log(error);
	return {
		type: ERROR,
		error
	};
}

export const fetchClasses = dispatch => {
	listClasses().then(
		(types) => dispatch({type: FETCHED_CLASSES, types}),
		err => dispatch(failWithError(err))
	);
};
