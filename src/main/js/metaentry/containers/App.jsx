import React, { Component } from 'react';
import { connect } from 'react-redux';
import TypesList from '../views/TypesListFactory.jsx';

class App extends Component {
	constructor(props) {
		super(props);
		this.state = {};
	}

	render() {
		const props = this.props;

		return <div className="row" style={{marginTop: "2px"}}>
			<div className="col-md-2"><TypesList {...props} /></div>
		</div>;
	}
}

function stateToProps(state){
	return Object.assign({}, state);
}

function dispatchToProps(dispatch){
	return {

	};
}

export default connect(stateToProps, dispatchToProps)(App)

