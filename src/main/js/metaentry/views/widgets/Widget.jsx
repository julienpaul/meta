import React, { Component } from 'react';
import WidgetButton from './WidgetButton.jsx';

export default class Widget extends Component {
	constructor(props) {
		super(props);
	}

	render() {
		const props = this.props;
		const cssClasses = "panel panel-" + props.widgetType;
		const buttons = props.buttons || [];

		return <div className={cssClasses}>

			<div className="panel-heading" title={props.headerTitle}>

				<h3 style={{display: "inline"}} className="panel-title">{props.widgetTitle}</h3>

				<div style={{display: "inline", float: "right"}}>{
					_.map(buttons, function(buttProps, i){
						var props = _.extend({}, buttProps, {key: "propButton_" + i});
						return <WidgetButton {...props}/>;
					})
				}</div>

			</div>

			<div className="panel-body">{props.children}</div>

		</div>;
	}
}
