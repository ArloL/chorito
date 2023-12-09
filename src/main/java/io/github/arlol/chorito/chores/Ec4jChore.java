package io.github.arlol.chorito.chores;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.ec4j.core.Cache.Caches;
import org.ec4j.core.Resource.Charsets;
import org.ec4j.core.Resource.Resources;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.lint.api.CustomLinterRegistryBuilder;
import org.ec4j.lint.api.FormattingHandler;
import org.ec4j.lint.api.Linter;
import org.ec4j.lint.api.LinterRegistry;
import org.ec4j.lint.api.Resource;
import org.ec4j.lint.api.ViolationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;

public class Ec4jChore implements Chore {

	private static final Logger LOG = LoggerFactory.getLogger(Ec4jChore.class);

	private static final org.ec4j.lint.api.Logger EC4J_LOGGER = org.ec4j.lint.api.Logger.NO_OP;

	@Override
	public void doit(ChoreContext context) {
		try {
			LinterRegistry linterRegistry = buildLinterRegistry(context.root());
			ViolationHandler handler = new FormattingHandler(
					false,
					".bak",
					EC4J_LOGGER
			);
			final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService
					.builder()
					.cache(Caches.permanent())
					.build();
			handler.startFiles();
			boolean propertyMatched = false;
			for (Path file : context.textFiles()) {
				final Path absFile = context.resolve(file);
				final ResourceProperties editorConfigProperties = resourcePropertiesService
						.queryProperties(Resources.ofPath(absFile, UTF_8));
				if (!editorConfigProperties.getProperties().isEmpty()) {
					propertyMatched = true;
					final Charset useEncoding = Charsets.forName(
							editorConfigProperties.getValue(
									PropertyType.charset,
									"UTF-8",
									true
							)
					);
					final Resource resource = new Resource(
							absFile,
							file,
							useEncoding
					);
					final List<Linter> filteredLinters = linterRegistry
							.filter(file);
					if (filteredLinters.isEmpty()) {
						continue;
					}
					ViolationHandler.ReturnState state = ViolationHandler.ReturnState.RECHECK;
					while (state != ViolationHandler.ReturnState.FINISHED) {
						for (Linter linter : filteredLinters) {
							handler.startFile(resource);
							linter.process(
									resource,
									editorConfigProperties,
									handler
							);
						}
						state = handler.endFile();
					}
				}
			}
			if (!propertyMatched) {
				LOG.error(
						"No .editorconfig properties applicable for files under '{}'",
						context.root()
				);
			}
			handler.endFiles();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private LinterRegistry buildLinterRegistry(Path currentDir) {
		return new CustomLinterRegistryBuilder(currentDir)
				.scan(getClass().getClassLoader())
				.log(EC4J_LOGGER)
				.build();
	}

}
