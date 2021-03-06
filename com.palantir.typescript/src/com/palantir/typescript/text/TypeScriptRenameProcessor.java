/*
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.typescript.text;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.palantir.typescript.EclipseResources;
import com.palantir.typescript.services.language.LanguageService;
import com.palantir.typescript.services.language.ReferenceEntry;

/**
 * The TypeScript rename processor.
 *
 * @author dcicerone
 */
public final class TypeScriptRenameProcessor extends RenameProcessor {

    private String newName;

    private final LanguageService languageService;
    private final String fileName;
    private final int offset;
    private final String oldName;

    public TypeScriptRenameProcessor(LanguageService languageService, String fileName, int offset, String oldName) {
        checkNotNull(languageService);
        checkNotNull(fileName);
        checkArgument(offset >= 0);
        checkNotNull(oldName);

        this.languageService = languageService;
        this.fileName = fileName;
        this.offset = offset;
        this.oldName = oldName;
    }

    @Override
    public Object[] getElements() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return "com.palantir.typescript.rename";
    }

    @Override
    public String getProcessorName() {
        return "Rename TypeScript Element";
    }

    @Override
    public boolean isApplicable() throws CoreException {
        return true;
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException,
            OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        List<ReferenceEntry> references = this.languageService.getReferencesAtPosition(this.fileName, this.offset);

        // group the references by file name
        ListMultimap<String, ReferenceEntry> referencesByFileName = ArrayListMultimap.create();
        for (ReferenceEntry reference : references) {
            referencesByFileName.put(reference.getFileName(), reference);
        }

        // create the file changes
        List<Change> fileChanges = Lists.newArrayList();
        for (String referenceFileName : referencesByFileName.keySet()) {
            List<ReferenceEntry> fileReferences = referencesByFileName.get(referenceFileName);
            IFile file = EclipseResources.getFile(referenceFileName);
            TextFileChange change = new TextFileChange(file.getName(), file);
            change.setEdit(new MultiTextEdit());
            change.setTextType("ts");

            for (ReferenceEntry reference : fileReferences) {
                int minChar = reference.getMinChar();
                int limChar = reference.getLimChar();
                ReplaceEdit edit = new ReplaceEdit(minChar, limChar - minChar, this.newName);

                change.addEdit(edit);
            }

            fileChanges.add(change);
        }

        return new CompositeChange("Rename TypeScript Element", fileChanges.toArray(new Change[fileChanges.size()]));
    }

    @Override
    public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
            throws CoreException {
        return null;
    }

    public String getOldName() {
        return this.oldName;
    }

    public void setNewName(String newName) {
        checkNotNull(newName);

        this.newName = newName;
    }
}
