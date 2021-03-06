import ContentPanel from './ContentPanel.jsx';
import {statusFullText, statusClass} from './StationsListFactory.jsx';
import {status} from '../models/ApplicationStatus.js';

function getTransitionText(from, to){
	switch(to){
		case status.acknowledged: return ["Acknowledge Step 1", "Acknowledge the Step 1 application"];
		case status.notSubmitted: return ["Return Step 1", "Return the Step 1 application for correction and resubmission"];
		case status.approved: return from === status.step2started
			? ["Cancel Step 2", "Reset the status back to \"Step 1 approved\""]
			: ["Approve Step 1", "Approve Step 1 of labeling for this station"];
		case status.rejected: return ["Reject Step 1", "Reject the application at Step 1"];
		case status.step2started:
			return from === status.approved
				? ["Start Step 2", "Start Step 2 of station labeling"]
				: ["Reset Step 2", "Change application status back to \"Started Step 2\""];
		case status.step2approved:
			return from === status.step3approved
				? ["Revoke final approval", "Reset the status back to \"Step 2 approved\""]
				: ["Approve Step 2", "Approve Step 2 of station's labeling procedure"];
		case status.step2rejected: return ["Reject Step 2", "Reject the application at Step 2"];
		case status.step3approved: return ["Grant final approval", "Make this an official ICOS station"];
		default: return ["Unsupported status", "Unsupported status: " + to];
	}
}

function transitionButtonClass(to){
	switch(to){
		case status.acknowledged: return "btn-primary";
		case status.notSubmitted: return "cp-btn-gray";
		case status.approved: return "btn-success";
		case status.rejected: return "btn-danger";
		case status.step2started : return "btn-warning";
		case status.step2approved : return "btn-success";
		case status.step2rejected : return "btn-danger";
		case status.step3approved : return "btn-success";
	}
}

export default function(updateStatusAction) {

	const LifecycleButton = React.createClass({

		render: function(){
			const props = this.props;

			return <button type="button" className={'btn ' + props.buttonClass}
					onClick={() => updateStatusAction(props.getUpdated())} title={props.tooltip} style={{marginRight: 5}}>{props.buttonName}</button>;
		}
	});

	const LifecycleControls = React.createClass({

		render: function(){
			let status = this.props.status;
			if(!status.canControlLifecycle) return null;

			return <div style={{marginTop: 15}}>{
				_.map(status.transitions, to => {
					let [buttonLabel, helpText] = getTransitionText(status.value, to);

					return <LifecycleButton key={to} buttonClass={transitionButtonClass(to)}
						getUpdated={() => status.stationWithStatus(to)}
						tooltip={helpText} buttonName={buttonLabel}
					/>;
				})
			}</div>;
		}

	});

	return React.createClass({

		render: function() {
			const appStatus = this.props.status.value;

			return <ContentPanel panelTitle="Application status">

				<h3 style={{marginTop: 0, marginBottom: 5}}>
					<span className={statusClass(appStatus)}>
						{statusFullText(appStatus)}
					</span>
				</h3>

				<LifecycleControls status={this.props.status} />

			</ContentPanel>;
		}
	});

};

