var ContentPanel = require('./ContentPanel.jsx');

function getValidatingMixin(){
	var validators = arguments;

	return {
		componentWillMount: function(){
			var self = this;
			self.validators = self.validators || [];
			_.each(validators, validator => self.validators.push(validator));
		}
	};
}

function fromMixins(){
	return React.createClass({mixins: arguments});
}

var InputBaseMixin = {

	componentWillMount: function(){
		this.validators = this.validators || [];
	},

	componentWillReceiveProps: function(newProps){
		if(this.props.value !== newProps.value) this.pushUpdate(newProps.value);
	},

	getErrors: function(value){
		var firstFailing = _.find(this.validators, validator => !_.isEmpty(validator(value)));
		return firstFailing ? firstFailing(value) : [];
	},

	changeHandler: function(event){
		var newValue = event.target.value;
		this.pushUpdate(newValue);
	},

	pushUpdate: function(newValue){
		var errors = this.getErrors(newValue);
		var finalValue = _.isEmpty(errors) ? this.extractUpdatedValue(newValue) : newValue;
		this.props.updater(errors, finalValue);
	}
};

var TextInputMixin = _.extend({
	render: function() {
		var errors = this.getErrors(this.props.value);

		var style = _.isEmpty(errors) ? {} : {backgroundColor: "pink"};

		return <input type="text" className="form-control" style={style} onChange={this.changeHandler} title={errors.join('\n')}
					  value={this.props.value} disabled={this.props.disabled} />;
	}
}, InputBaseMixin);

var TextAreaMixin = _.extend({
	render: function() {
		var errors = this.getErrors(this.props.value);

		var style = _.extend(_.isEmpty(errors) ? {} : {backgroundColor: "pink"}, this.inputStyle);

		return <textarea rows="3" className="form-control" style={style} onChange={this.changeHandler} title={errors.join('\n')}
					  value={this.props.value} disabled={this.props.disabled} />;
	}
}, InputBaseMixin);

var DropDownMixin = _.extend({
	render: function() {
		return <select className="form-control" value={this.props.value} disabled={this.props.disabled} onChange={this.changeHandler}>{
			_.map(this.props.options, (text, value) =>
				<option value={value} key={value}>{text}</option>
			)
		}</select>;
	}
}, InputBaseMixin);

var StringInputMixin = {extractUpdatedValue: _.identity};
var NumberInputMixin = {extractUpdatedValue: s => parseFloat(s)};

var IsNumberMixin = getValidatingMixin(value => {
	return (parseFloat(value).toString() === value.toString()) ? [] : ["Not a valid number!"];
});

var IsUrlMixin = getValidatingMixin(value => {
	if (value == undefined || value.length == 0){
		return [];
	} else {
		return /^(https?):\/\//i.test(value) ? [] : ["The URL must begin with http:// or https://"];
	}
});

var IsPhoneMixin = getValidatingMixin(value => {
	if(_.isEmpty(value)) return [];
	return /\+[\d\s]{8,}/.test(value) ? [] : ["Must be a phone number in the international format +XXXXXXXX (spaces allowed)"];
});

var IsNotEmpty = getValidatingMixin(value => {
	return (value !== undefined && value.length > 0) ? [] : ["Required field. It must be filled in."];
});

function hasMinValue(minValue){
	return getValidatingMixin(value => {
		var num = parseFloat(value);
		return num >= minValue ? [] : ["Value must not be less than " + minValue];
	});
}

function hasMaxValue(maxValue){
	return getValidatingMixin(value => {
		var num = parseFloat(value);
		return num <= maxValue ? [] : ["Value must not exceed " + maxValue];
	});
}


module.exports = {

	Number: fromMixins(TextInputMixin, NumberInputMixin, IsNumberMixin),

	Latitude: fromMixins(TextInputMixin, NumberInputMixin, IsNumberMixin, hasMinValue(-90), hasMaxValue(90)),

	Longitude: fromMixins(TextInputMixin, NumberInputMixin, IsNumberMixin, hasMinValue(-180), hasMaxValue(180)),

	URL: fromMixins(TextInputMixin, StringInputMixin, IsUrlMixin),
	Phone: fromMixins(TextInputMixin, StringInputMixin, IsPhoneMixin),

	String: fromMixins(TextInputMixin, StringInputMixin),

	StringRequired: fromMixins(TextInputMixin, StringInputMixin, IsNotEmpty),

	TextArea: fromMixins(TextAreaMixin, StringInputMixin),

	TextAreaRequired: fromMixins(TextAreaMixin, StringInputMixin, IsNotEmpty),

	DropDownString: fromMixins(DropDownMixin, StringInputMixin),

	Header: React.createClass({
		render: function() {
			return <div className="row">
				<div className="col-md-2">&nbsp;</div>
				<div className="col-md-10">
					<h4>{this.props.title}</h4>
				</div>
			</div>;
		}
	}),

	Group: React.createClass({
		render: function() {
			return <div className="row">
				<div className="form-group col-md-2">
					<label>{this.props.title}</label>
				</div>
				<div className="form-group col-md-10">
					{this.props.children}
				</div>
			</div>;
		}
	}),

	FormForm: React.createClass({
		render: function(){
			if(_.isEmpty(this.props.children)) return null;

			return <ContentPanel panelTitle="Station properties">
				<form role="form" onSubmit={this.props.submissionHandler}>
					{this.props.children}
					<button type="submit" className="btn btn-primary" disabled={!this.props.canSave}>Save</button>
				</form>
			</ContentPanel>;
		}
	})
};
