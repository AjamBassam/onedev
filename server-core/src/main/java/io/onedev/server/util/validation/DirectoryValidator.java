package io.onedev.server.util.validation;

import java.io.File;
import java.util.function.Function;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.onedev.commons.launcher.bootstrap.Bootstrap;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.StringUtils;
import io.onedev.server.util.interpolative.Interpolative;
import io.onedev.server.util.validation.annotation.Directory;

public class DirectoryValidator implements ConstraintValidator<Directory, String> {

	private Directory annotation;
	
	@Override
	public void initialize(Directory constaintAnnotation) {
		annotation = constaintAnnotation;
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext constraintContext) {
		if (value == null)
			return true;

		if (annotation.interpolative() && !Interpolated.get()) try {
			value = StringUtils.unescape(Interpolative.fromString(value).interpolateWith(new Function<String, String>() {

				@Override
				public String apply(String t) {
					return "a";
				}
				
			}));
		} catch (Exception e) {
			return true; // will be handled by interpolative validator
		}
		
		try {
			File dir = new File(value);
			if (annotation.absolute()) {
				if (!dir.isAbsolute()) {
					constraintContext.disableDefaultConstraintViolation();
					String message = annotation.message();
					if (message.length() == 0)
						message = "Please specify an absolute directory";
					constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
					return false;
				}
			}
			if (annotation.writeable()) {
				if (!FileUtils.isWritable(dir)) {
					constraintContext.disableDefaultConstraintViolation();
					String message = annotation.message();
					if (message.length() == 0)
						message = "Directory is not writeable";
					constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
					return false;
				}
			}
			if (annotation.outsideOfInstallDir()) {
				if (dir.getCanonicalFile().toPath().startsWith(Bootstrap.installDir.toPath())) {
					constraintContext.disableDefaultConstraintViolation();
					String message = annotation.message();
					if (message.length() == 0)
						message = "Please specify a directory outside of the installation directory";
					constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			constraintContext.disableDefaultConstraintViolation();
			String message = annotation.message();
			if (message.length() == 0)
				message = "Invalid directory";
			constraintContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
			return false;
		}
	}
}
