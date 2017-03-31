import React, { Component } from 'react';
import { connect } from 'react-redux';
import TypesList from '../views/TypesListFactory.jsx';
import * as Toaster from 'icos-cp-toaster';

class App extends Component {
	constructor(props) {
		super(props);
		this.state = {};
	}

	componentWillReceiveProps(nextProps){
		const toasterData = nextProps.toasterData;
		if(toasterData) this.setState({toasterData});
	}

	toastInfo(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_INFO, mess)});
	}

	toastWarning(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_WARNING, mess)});
	}

	toastError(mess){
		this.setState({toasterData: new Toaster.ToasterData(Toaster.TOAST_ERROR, mess)});
	}

	render() {
		const props = this.props;

		return (
			<div>
				<Toaster.AnimatedToasters
					autoCloseDelay={5000}
					fadeInTime={100}
					fadeOutTime={400}
					toasterData={this.state.toasterData}
					maxWidth={400}
				/>
				<div className="row" style={{marginTop: "2px"}}>
					<div className="col-md-2"><TypesList {...props} /></div>
				</div>
			</div>
		);
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

