import React, { Component } from 'react';
import ReactDOM from 'react-dom';

export default class ScreenHeightColumn extends Component{
	constructor(props) {
		super(props);
	}

	componentDidMount(){
		const self = this;
		self.ensureScreenHeight();
		this.resizeListener = _.throttle(
			function(){
				self.ensureScreenHeight();
			},
			200,
			{leading: false}
		),
			window.addEventListener("resize", this.resizeListener);
	}

	componentWillUnmount(){
		window.removeEventListener("resize", this.resizeListener);
	}

	ensureScreenHeight(){
		var listElem = ReactDOM.findDOMNode(this);

		var listRect = listElem.getBoundingClientRect();
		var panelRect = listElem.parentElement.parentElement.getBoundingClientRect();

		var totalMargin = panelRect.height - listRect.height;

		var desiredHeight = window.innerHeight - totalMargin - 10;

		listElem.style.height = desiredHeight + "px";
	}

	render(){
		return <div className={this.props.className} style={{overflowY: "auto", overflowX: "hidden"}}>
			{this.props.children}
		</div>;
	}
}
