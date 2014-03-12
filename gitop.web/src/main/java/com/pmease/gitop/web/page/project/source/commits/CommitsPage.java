package com.pmease.gitop.web.page.project.source.commits;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.parboiled.common.Preconditions;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pmease.commons.git.Commit;
import com.pmease.commons.git.Git;
import com.pmease.commons.git.command.LogCommand;
import com.pmease.gitop.model.Project;
import com.pmease.gitop.web.page.PageSpec;
import com.pmease.gitop.web.page.project.ProjectCategoryPage;
import com.pmease.gitop.web.page.project.api.IRevisionAware;
import com.pmease.gitop.web.page.project.source.component.RevisionSelector;

@SuppressWarnings("serial")
public class CommitsPage extends ProjectCategoryPage implements IRevisionAware {

	public static final int COMMITS_PER_PAGE = 30;
	
	public static PageParameters newParams(Project project, String revision, List<String> paths, int page) {
		Preconditions.checkNotNull(project);
		PageParameters params = PageSpec.forProject(project);
		if (!Strings.isNullOrEmpty(revision)) {
			params.set("objectId", revision);
		}
		
		for (int i = 0; i < paths.size(); i++) {
			params.set(i, paths.get(i));
		}
		
		if (page > 0) {
			params.set("page", page);
		}
		
		return params;
	}
	
	private int page;
	
	private final IModel<List<Commit>> commitsModel;
	private final IModel<List<String>> pathsModel;
	
	public CommitsPage(PageParameters params) {
		super(params);
		
		page = params.get("page").toInt(1);
		
		pathsModel = new LoadableDetachableModel<List<String>>() {

			@Override
			public List<String> load() {
				PageParameters params = CommitsPage.this.getPageParameters();
				int count = params.getIndexedCount();
				List<String> paths = Lists.newArrayList();
				for (int i = 0; i < count; i++) {
					String p = params.get(i).toString();
					if (!Strings.isNullOrEmpty(p)) {
						paths.add(p);
					}
				}
				
				return paths;
			}
		};
		
		commitsModel = new LoadableDetachableModel<List<Commit>>() {

			@Override
			protected List<Commit> load() {
				Git git = getProject().code();
				
				List<String> paths = getPaths();
				
				// load additional one commit to see whether there is still more page
				LogCommand command = new LogCommand(git.repoDir())
										.toRev(getRevision())
										.skip((page - 1) * COMMITS_PER_PAGE)
										.maxCount(COMMITS_PER_PAGE + 1);
				if (!paths.isEmpty()) {
					String p = Joiner.on("/").join(paths);
					command.path(p);
				}
				
				List<Commit> commits = command.call();
				return commits;
			}
		};
	}

	protected List<String> getPaths() {
		return pathsModel.getObject();
	}
	
	@Override
	protected void onPageInitialize() {
		super.onPageInitialize();
		
		add(new RevisionSelector("revselector", projectModel, revisionModel, null));
//		BookmarkablePageLink<Void> homeLink = new BookmarkablePageLink<Void>("home", 
//				SourceTreePage.class, 
//				PageSpec.forProject(getProject()).add(PageSpec.OBJECT_ID, getRevision()));
//		add(homeLink);
//		homeLink.add(new Label("name", new AbstractReadOnlyModel<String>() {
//
//			@Override
//			public String getObject() {
//				return getProject().getName();
//			}
//		}));

		add(newNavLink("home", -1));
		add(new ListView<String>("paths", pathsModel) {

			@Override
			protected void populateItem(ListItem<String> item) {
				item.add(newNavLink("link", item.getIndex()));
			}
		});
		
		add(new CommitsPanel("commits", commitsModel, projectModel));
		add(new BookmarkablePageLink<Void>("newer", CommitsPage.class,
				newParams(getProject(), getRevision(), getPaths(), page - 1)).setEnabled(page > 1));
		add(new BookmarkablePageLink<Void>("older", CommitsPage.class,
				newParams(getProject(), getRevision(), getPaths(), page + 1)) {
			@Override
			protected void onConfigure() {
				super.onConfigure();
				setEnabled(commitsModel.getObject().size() > COMMITS_PER_PAGE);
			}
			
		});
	}
	
	private Component newNavLink(String id, final int pathNum) {
		List<String> all = getPaths();
		List<String> paths = Lists.newArrayList();
		if (pathNum >= 0) {
			paths = all.subList(0, pathNum + 1);
		}
		
		BookmarkablePageLink<Void> link = new BookmarkablePageLink<Void>(id,
				CommitsPage.class,
				CommitsPage.newParams(getProject(), getRevision(), paths, 0));
		link.setEnabled(pathNum < 0 || pathNum < all.size() - 1);
		link.add(new Label("name", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (pathNum < 0) {
					return getProject().getName();
				} else {
					List<String> all = getPaths();
					return all.get(pathNum);
				}
			}
			
		}));
		
		return link;
	}
	@Override
	protected String getPageTitle() {
		return "Commits - " + getProject();
	}

	@Override
	public void onDetach() {
		if (commitsModel != null) {
			commitsModel.detach();
		}

		if (pathsModel != null) {
			pathsModel.detach();
		}
		
		super.onDetach();
	}
}
