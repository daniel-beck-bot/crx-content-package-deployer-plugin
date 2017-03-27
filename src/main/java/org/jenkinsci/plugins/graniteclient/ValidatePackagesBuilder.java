/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package org.jenkinsci.plugins.graniteclient;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.WspFilter;
import net.adamcin.granite.client.packman.validation.DefaultValidationOptions;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.*;
import javax.annotation.Nonnull;

/**
 * Implementation of the "Validate CRX Content Packages" build step
 */
public class ValidatePackagesBuilder extends AbstractBuildStep {

    private String packageIdFilters;
    private String localDirectory;
    private String validationFilter;
    private boolean allowNonCoveredRoots;
    private String forbiddenExtensions;
    private String forbiddenACHandlingModeSet;
    private String forbiddenFilterRootPrefixes;
    private String pathsDeniedForInclusion;

    @DataBoundConstructor
    public ValidatePackagesBuilder(String packageIdFilters, String localDirectory, String validationFilter,
                                   boolean allowNonCoveredRoots, String forbiddenExtensions, String forbiddenACHandlingModeSet,
                                   String forbiddenFilterRootPrefixes, String pathsDeniedForInclusion) {
        this.packageIdFilters = packageIdFilters;
        this.localDirectory = localDirectory;
        this.validationFilter = validationFilter;
        this.allowNonCoveredRoots = allowNonCoveredRoots;
        this.forbiddenExtensions = forbiddenExtensions;
        this.forbiddenACHandlingModeSet = forbiddenACHandlingModeSet;
        this.forbiddenFilterRootPrefixes = forbiddenFilterRootPrefixes;
        this.pathsDeniedForInclusion = pathsDeniedForInclusion;
    }

    public String getPackageIdFilters() {
        //return packageIdFilters;
        if (packageIdFilters != null) {
            return packageIdFilters.trim();
        } else {
            return "";
        }
    }

    public void setPackageIdFilters(String packageIdFilters) {
        this.packageIdFilters = packageIdFilters;
    }


    public String getLocalDirectory() {
        if (localDirectory == null || localDirectory.trim().isEmpty()) {
            return ".";
        } else {
            return localDirectory.trim();
        }
    }

    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }

    public String getValidationFilter() {
        return validationFilter;
    }

    public void setValidationFilter(String validationFilter) {
        this.validationFilter = validationFilter;
    }

    public boolean isAllowNonCoveredRoots() {
        return allowNonCoveredRoots;
    }

    public void setAllowNonCoveredRoots(boolean allowNonCoveredRoots) {
        this.allowNonCoveredRoots = allowNonCoveredRoots;
    }

    public String getForbiddenExtensions() {
        return forbiddenExtensions;
    }

    public void setForbiddenExtensions(String forbiddenExtensions) {
        this.forbiddenExtensions = forbiddenExtensions;
    }

    public String getForbiddenACHandlingModeSet() {
        return forbiddenACHandlingModeSet;
    }

    public void setForbiddenACHandlingModeSet(String forbiddenACHandlingModeSet) {
        this.forbiddenACHandlingModeSet = forbiddenACHandlingModeSet;
    }

    public String getForbiddenFilterRootPrefixes() {
        return forbiddenFilterRootPrefixes;
    }

    public void setForbiddenFilterRootPrefixes(String forbiddenFilterRootPrefixes) {
        this.forbiddenFilterRootPrefixes = forbiddenFilterRootPrefixes;
    }

    public String getPathsDeniedForInclusion() {
        return pathsDeniedForInclusion;
    }

    public void setPathsDeniedForInclusion(String pathsDeniedForInclusion) {
        this.pathsDeniedForInclusion = pathsDeniedForInclusion;
    }

    public String getValidationFilter(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        try {
            return TokenMacro.expandAll(build, listener, getValidationFilter());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getValidationFilter());
        }
        return getValidationFilter();
    }

    public String getForbiddenExtensions(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        try {
            return TokenMacro.expandAll(build, listener, getForbiddenExtensions());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getForbiddenExtensions());
        }
        return getForbiddenExtensions();
    }

    public String getPathsDeniedForInclusion(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        try {
            return TokenMacro.expandAll(build, listener, getPathsDeniedForInclusion());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getPathsDeniedForInclusion());
        }
        return getPathsDeniedForInclusion();
    }

    public String getForbiddenFilterRootPrefixes(AbstractBuild<?, ?> build, TaskListener listener) throws IOException, InterruptedException {
        try {
            return TokenMacro.expandAll(build, listener, getForbiddenFilterRootPrefixes());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getForbiddenFilterRootPrefixes());
        }
        return getForbiddenFilterRootPrefixes();
    }

    public List<ACHandling> getForbiddenACHandlingModes() {
        return ForbiddenACHandlingModeSet.safeValueOf(getForbiddenACHandlingModeSet()).getForbiddenModes();
    }

    private DefaultValidationOptions getValidationOptions(AbstractBuild<?, ?> build, TaskListener listener)
            throws IOException, InterruptedException {
        DefaultValidationOptions options = new DefaultValidationOptions();
        options.setAllowNonCoveredRoots(isAllowNonCoveredRoots());
        options.setForbiddenExtensions(Arrays.asList(getForbiddenExtensions(build, listener).split("\r?\n")));
        options.setPathsDeniedForInclusion(Arrays.asList(getPathsDeniedForInclusion(build, listener).split("\r?\n")));
        options.setForbiddenACHandlingModes(getForbiddenACHandlingModes());
        options.setForbiddenFilterRootPrefixes(Arrays.asList(getForbiddenFilterRootPrefixes(build, listener).split("\r?\n")));

        WspFilter filter = WspFilter.parseSimpleSpec(getValidationFilter(build, listener));
        if (filter != null && !filter.getRoots().isEmpty()) {
            options.setValidationFilter(filter);
        }
        return options;
    }

    @Override
    boolean perform(@Nonnull AbstractBuild<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                    @Nonnull BuildListener listener) throws InterruptedException, IOException {

        Result result = build.getResult();
        if (result == null) {
            result = Result.SUCCESS;
        }

        listener.getLogger().println("Validating packages.");

        DefaultValidationOptions options = getValidationOptions(build, listener);
        for (PackTuple selectedPackage : selectPackages(build, workspace, listener)) {
            listener.getLogger().printf("Validating package %s at path %s.%n",
                    selectedPackage.getPackId(), selectedPackage.getFilePath());

            FilePath.FileCallable<Result> callable =
                    new ValidateFileCallable(listener, options);

            result = result.combine(selectedPackage.getFilePath().act(callable));
            build.setResult(result);
        }

        return result.isBetterOrEqualTo(Result.UNSTABLE);
    }

    static class PackTuple {
        final PackId packId;
        final FilePath filePath;
        final boolean pathOnly;

        PackTuple(PackId packId, FilePath filePath) {
            this.packId = packId;
            this.filePath = filePath;
            this.pathOnly = this.packId == null;
        }

        public PackId getPackId() {
            return packId;
        }

        public FilePath getFilePath() {
            return filePath;
        }

        public boolean isPathOnly() {
            return pathOnly;
        }
    }

    private List<PackTuple> selectPackages(@Nonnull final AbstractBuild<?, ?> build, @Nonnull final FilePath workspace,
                                           @Nonnull final BuildListener listener)
            throws IOException, InterruptedException {
        List<PackTuple> found = new ArrayList<PackTuple>();

        final String fLocalDirectory = getLocalDirectory(build, listener);
        FilePath dir = workspace.child(fLocalDirectory);

        try {
            List<FilePath> listed = new ArrayList<FilePath>();
            listed.addAll(Arrays.asList(dir.list("**/*.jar")));
            listed.addAll(Arrays.asList(dir.list("**/*.zip")));

            for (FilePath path : listed) {
                PackId packId = null;
                try {
                    packId = path.act(new IdentifyPackageCallable());
                } catch (Exception e) {
                    listener.error("Failed to identify package file: %s", e.getMessage());
                }
                found.add(new PackTuple(packId, path));
            }
        } catch (Exception e) {
            listener.error("Failed to list package files: %s", e.getMessage());
        }

        List<PackTuple> selected = new ArrayList<PackTuple>();
        for (Map.Entry<String, PathOrPackIdFilter> filterEntry : listPackageFilters(build, listener).entrySet()) {
            boolean matched = false;
            for (PackTuple entry : found) {
                if (!entry.isPathOnly() &&
                        filterEntry.getValue().includes(entry.getPackId())) {
                    matched = true;
                    selected.add(entry);
                } else if (filterEntry.getValue().includes(dir, entry.getFilePath())) {
                    matched = true;
                    selected.add(entry);
                }
            }

            if (!matched) {
                throw new IOException("No package found matching filter " + filterEntry.getKey());
            }
        }

        return Collections.unmodifiableList(selected);
    }

    public String getPackageIdFilters(AbstractBuild<?, ?> build, TaskListener listener) throws Exception {
        return TokenMacro.expandAll(build, listener, getPackageIdFilters());
    }

    private Map<String, PathOrPackIdFilter> listPackageFilters(AbstractBuild<?, ?> build, TaskListener listener) {
        Map<String, PathOrPackIdFilter> filters = new LinkedHashMap<String, PathOrPackIdFilter>();
        try {
            for (String filter : BaseUrlUtil.splitByNewline(getPackageIdFilters(build, listener))) {
                if (filter.trim().length() > 0) {
                    filters.put(filter, PathOrPackIdFilter.parse(filter));
                }
            }
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %n%s", getPackageIdFilters());
        }
        return Collections.unmodifiableMap(filters);
    }

    private String getLocalDirectory(AbstractBuild<?, ?> build, TaskListener listener) {
        try {
            return TokenMacro.expandAll(build, listener, getLocalDirectory());
        } catch (Exception e) {
            listener.error("failed to expand tokens in: %s%n", getLocalDirectory());
        }
        return getLocalDirectory();
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Validate CRX Content Packages";
        }

        public FormValidation doCheckValidationFilter(@QueryParameter String value) {
            try {
                WspFilter.parseSimpleSpec(value);
                return FormValidation.ok();
            } catch (RuntimeException e) {
                return FormValidation.error(e.getMessage());
            }
        }


        public ListBoxModel doFillForbiddenACHandlingModeSetItems() {
            ListBoxModel model = new ListBoxModel();
            for (ForbiddenACHandlingModeSet modeSet : ForbiddenACHandlingModeSet.values()) {
                model.add(modeSet.getListLabel(), modeSet.name());
            }
            return model;
        }

    }

    enum ForbiddenACHandlingModeSet {
        SKIP_VALIDATION("Skip Validation", Collections.<ACHandling>emptyList()),
        NO_CLEAR("No Clear", Collections.singletonList(ACHandling.CLEAR)),
        NO_UNSAFE("No Unsafe", Arrays.asList(ACHandling.CLEAR, ACHandling.OVERWRITE)),
        ALLOW_ADDITIVE("Allow Additive", Arrays.asList(ACHandling.CLEAR, ACHandling.OVERWRITE, ACHandling.MERGE)),
        NO_ACL("No ACLs", Arrays.asList(ACHandling.CLEAR, ACHandling.OVERWRITE, ACHandling.MERGE, ACHandling.MERGE_PRESERVE));

        private final String listLabel;
        private final List<ACHandling> forbiddenModes;

        ForbiddenACHandlingModeSet(String listLabel, List<ACHandling> forbiddenModes) {
            this.listLabel = listLabel;
            this.forbiddenModes = forbiddenModes;
        }

        public String getListLabel() {
            return listLabel;
        }

        public List<ACHandling> getForbiddenModes() {
            return forbiddenModes;
        }

        static ForbiddenACHandlingModeSet forEmptySelection() {
            return SKIP_VALIDATION;
        }

        static ForbiddenACHandlingModeSet forUnknownSelection() {
            return NO_ACL;
        }

        static ForbiddenACHandlingModeSet safeValueOf(String modeSetName) {
            if (modeSetName == null || modeSetName.isEmpty()) {
                return forEmptySelection();
            } else {
                try {
                    return ForbiddenACHandlingModeSet.valueOf(modeSetName);
                } catch (IllegalArgumentException e) {
                    if (modeSetName.toLowerCase().startsWith("skip")) {
                        return forEmptySelection();
                    } else {
                        return forUnknownSelection();
                    }
                }
            }
        }
    }
}
