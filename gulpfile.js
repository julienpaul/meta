'use strict';

var gulp = require('gulp');
var gp_uglify = require('gulp-uglify');
var browserify = require('browserify');
var buffer = require('vinyl-buffer');
var del = require('del');
var source = require('vinyl-source-stream');
var babelify = require('babelify');
var babel = require('gulp-babel');
var preprocessify = require('preprocessify');

['metaentry', 'labeling', 'sparqlclient'].forEach(function(project){

	var paths = {
		main: 'src/main/js/' + project + '/main.jsx',
		jsx: ['src/main/js/' + project + '/**/*.jsx'],
		js: ['src/main/js/' + project + '/**/*.js'],
		alljs: 'src/main/js/' + '/**/*.js',
		commonjs: 'src/main/js/common/**/*.js*',
		commonjsx: 'src/main/js/common/**/*.jsx*',
		target: 'src/main/resources/www/',
		bundleFile: project + '.js'
	};

	gulp.task('clean' + project, function() {
		return del([paths.target + paths.bundleFile]);
	});

	gulp.task('apply-prod-environment', function() {
		process.env.NODE_ENV = 'production';
	});

	function compileJs() {
		var browser = browserify({
			entries: [paths.main],
			debug: false
		})
			.transform(preprocessify({NODE_ENV: process.env.NODE_ENV}))
			.transform(babelify, {presets: ["es2015", "react"]})
			.bundle()
			.on('error', function(err){
				console.log(err);
				this.emit('end');
			});

		if (process.env.NODE_ENV === 'production'){
			return browser
				.pipe(source(paths.bundleFile))
				.pipe(buffer())
				.pipe(gp_uglify())
				.pipe(gulp.dest(paths.target));
		} else {
			return browser
				.pipe(source(paths.bundleFile))
				.pipe(gulp.dest(paths.target));
		}
	}

	gulp.task('js' + project, ['clean' + project], compileJs);

	gulp.task('clean_es5' + project, function() {
		return del([paths.target]);
	});

	gulp.task('transpile' + project, ['clean_es5' + project], function(){
		return gulp.src(paths.alljs)
			.pipe(babel({presets: ['es2015']}))
			.pipe(gulp.dest(paths.target));
	});

	gulp.task(project, ['js' + project], function(){
		var sources = [paths.commonjs, paths.commonjsx, paths.js, paths.jsx];
		return gulp.watch(sources, ['js' + project]);
	});

	gulp.task('publish' + project, ['apply-prod-environment', 'clean' + project], compileJs);

});


