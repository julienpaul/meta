import React, { Component } from 'react';
import ensureLength from '../utils';
import Widget from './widgets/Widget.jsx';
import ScreenHeightColumn from './ScreenHeightColumn.jsx';

const capped = ensureLength.ensureLength(30);

export default class TypesListFactory extends Component{
	constructor(props) {
		super(props);
	}

	render(){
		const props = this.props;
		console.log({props});

		return (
			<div>
				<Widget widgetType="primary" widgetTitle="Types">
					<ScreenHeightColumn>
						<div className="list-group">{

							props.types.map(function(theType){

								var clickHandler = _.partial(chooseTypeAction, theType.uri);
								var isChosen = (theType.uri == props.chosen);
								var fullName = theType.displayName;

								return <li
									className={"cp-lnk list-group-item list-group-item-" + (isChosen ? "info" : "default")}
									key={theType.uri} title={fullName} onClick={clickHandler}>
									{capped(fullName)}
								</li>;
							})

						}</div>
					</ScreenHeightColumn>
				</Widget>
			</div>
		);
	}
}
