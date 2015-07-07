package il.ac.technion.cs.ssdl.spartan.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

import il.ac.technion.cs.ssdl.spartan.utils.As;
import il.ac.technion.cs.ssdl.spartan.utils.Make;
import il.ac.technion.cs.ssdl.spartan.utils.Range;

/**
 * the base class for all Spartanization Refactoring classes, contains common
 * functionality
 *
 * @author Artium Nihamkin (original)
 * @author Boris van Sosin <boris.van.sosin [at] gmail.com>} (v2)
 * @author Yossi Gil <code><yossi.gil [at] gmail.com></code>: major refactoring
 *         2013/07/10
 *
 * @since 2013/01/01
 */
public abstract class Spartanization extends Refactoring {
  protected abstract ASTVisitor fillOpportunities(final List<Range> opportunities);

  protected abstract void fillRewrite(ASTRewrite r, AST t, CompilationUnit cu, IMarker m);

  private ITextSelection selection = null;
  private ICompilationUnit compilationUnit = null;
  private IMarker marker = null;
  final Collection<TextFileChange> changes = new ArrayList<>();
  private final String name;
  private final String message;

  /***
   * Instantiates this class, with message identical to name
   *
   * @param name
   *          a short name of this refactoring
   */
  protected Spartanization(final String name) {
    this(name, name);
  }

  /***
   * Instantiates this class
   *
   * @param name
   *          a short name of this refactoring
   * @param message
   *          the message to display in the quickfix
   */
  protected Spartanization(final String name, final String message) {
    this.name = name;
    this.message = message;
  }

  @Override public final String getName() {
    return name;
  }

  /**
   * creates an ASTRewrite which contains the changes
   *
   * @param cu
   *          the Compilation Unit (outermost ASTNode in the Java Grammar)
   * @param pm
   *          a progress monitor in which to display the progress of the
   *          refactoring
   * @return an ASTRewrite which contains the changes
   */
  public final ASTRewrite createRewrite(final CompilationUnit cu, final SubProgressMonitor pm) {
    return createRewrite(pm, cu.getAST(), cu, (IMarker) null);
  }

  /**
   * creates an ASTRewrite, under the context of a text marker, which contains
   * the changes
   *
   * @param pm
   *          a progress monitor in which to display the progress of the
   *          refactoring
   * @param m
   *          the marker
   * @return an ASTRewrite which contains the changes
   */
  private final ASTRewrite createRewrite(final SubProgressMonitor pm, final IMarker m) {
    return createRewrite(pm, (CompilationUnit) As.COMPILIATION_UNIT.ast(m, pm), m);
  }

  private ASTRewrite createRewrite(final SubProgressMonitor pm, final CompilationUnit cu, final IMarker m) {
    return createRewrite(pm, cu.getAST(), cu, m);
  }

  private ASTRewrite createRewrite(final SubProgressMonitor pm, final AST t, final CompilationUnit cu, final IMarker m) {
    if (pm != null)
      pm.beginTask("Creating rewrite operation...", 1);
    final ASTRewrite $ = createRewrite(t, cu, m);
    if (pm != null)
      pm.done();
    return $;
  }

  private ASTRewrite createRewrite(final AST t, final CompilationUnit cu, final IMarker m) {
    final ASTRewrite $ = ASTRewrite.create(t);
    fillRewrite($, t, cu, m);
    return $;
  }

  private final boolean isTextSelected() {
    return selection != null && !selection.isEmpty() && selection.getLength() != 0;
  }

  /**
   * Determines if the node is outside of the selected text.
   *
   * @return true if the node is not inside selection. If there is no selection
   *         at all will return false.
   */
  protected boolean isNodeOutsideSelection(final ASTNode n) {
    return !isSelected(n.getStartPosition());
  }

  private boolean isSelected(final int offset) {
    return isTextSelected() && offset >= selection.getOffset() && offset < selection.getOffset() + selection.getLength();
  }

  protected static boolean isNodeOutsideMarker(final ASTNode n, final IMarker m) {
    try {
      return n.getStartPosition() < ((Integer) m.getAttribute(IMarker.CHAR_START)).intValue()
          || n.getStartPosition() + n.getLength() > ((Integer) m.getAttribute(IMarker.CHAR_END)).intValue();
    } catch (final CoreException e) {
      return true;
    }
  }

  @Override public RefactoringStatus checkInitialConditions(@SuppressWarnings("unused") final IProgressMonitor pm) {
    final RefactoringStatus $ = new RefactoringStatus();
    if (compilationUnit == null && marker == null)
      $.merge(RefactoringStatus.createFatalErrorStatus("Nothing to refactor."));
    return $;
  }

  /**
   * @param marker
   *          the marker to set for the refactoring
   */
  public final void setMarker(final IMarker marker) {
    this.marker = marker;
  }

  @Override public RefactoringStatus checkFinalConditions(final IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    changes.clear();
    if (marker == null)
      runAsManualCall(pm);
    else {
      innerRunAsMarkerFix(pm, marker, true);
      marker = null; // consume marker
    }
    pm.done();
    return new RefactoringStatus();
  }

  private void runAsManualCall(final IProgressMonitor pm) throws JavaModelException, CoreException {
    pm.beginTask("Checking preconditions...", 2);
    scanCompilationUnits(getUnits(pm), new SubProgressMonitor(pm, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
  }

  private List<ICompilationUnit> getUnits(final IProgressMonitor pm) throws JavaModelException {
    if (!isTextSelected())
      return getAllProjectCompilationUnits(new SubProgressMonitor(pm, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
    final List<ICompilationUnit> $ = new ArrayList<>();
    $.add(compilationUnit);
    return $;
  }

  /**
   * @param pm
   *          a progress monitor in which to display the progress of the
   *          refactoring
   * @param m
   *          the marker for which the refactoring needs to run
   * @return a RefactoringStatus
   * @throws CoreException
   *           the JDT core throws it
   */
  public RefactoringStatus runAsMarkerFix(final IProgressMonitor pm, final IMarker m) throws CoreException {
    return innerRunAsMarkerFix(pm, m, false);
  }

  private RefactoringStatus innerRunAsMarkerFix(final IProgressMonitor pm, final IMarker m, final boolean preview)
      throws CoreException {
    marker = m;
    pm.beginTask("Running refactoring...", 2);
    scanCompilationUnitForMarkerFix(m, pm, preview);
    marker = null;
    pm.done();
    return new RefactoringStatus();
  }

  /**
   * Creates a change from each compilation unit and stores it in the changes
   * array
   *
   * @throws IllegalArgumentException
   * @throws CoreException
   */
  protected void scanCompilationUnits(final List<ICompilationUnit> cus, final IProgressMonitor pm)
      throws IllegalArgumentException, CoreException {
    pm.beginTask("Iterating over gathered compilation units...", cus.size());
    for (final ICompilationUnit cu : cus)
      scanCompilationUnit(cu, new SubProgressMonitor(pm, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
    pm.done();
  }

  /**
   * @param u
   * @throws CoreException
   */
  protected void scanCompilationUnit(final ICompilationUnit u, final IProgressMonitor m) throws CoreException {
    m.beginTask("Creating change for a single compilation unit...", 2);
    final TextFileChange textChange = new TextFileChange(u.getElementName(), (IFile) u.getResource());
    textChange.setTextType("java");
    final SubProgressMonitor subProgressMonitor = new SubProgressMonitor(m, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
    textChange
        .setEdit(createRewrite((CompilationUnit) Make.COMPILIATION_UNIT.parser(u).createAST(subProgressMonitor), subProgressMonitor)
            .rewriteAST());
    if (textChange.getEdit().getLength() != 0)
      changes.add(textChange);
    m.done();
  }

  protected void scanCompilationUnitForMarkerFix(final IMarker m, final IProgressMonitor pm, final boolean preview)
      throws CoreException {
    pm.beginTask("Creating change(s) for a single compilation unit...", 2);
    final ICompilationUnit u = As.iCompilationUnit(m);
    final TextFileChange textChange = new TextFileChange(u.getElementName(), (IFile) u.getResource());
    textChange.setTextType("java");
    textChange.setEdit(createRewrite(new SubProgressMonitor(pm, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL), m).rewriteAST());
    if (textChange.getEdit().getLength() != 0)
      if (preview)
        changes.add(textChange);
      else textChange.perform(pm);
    pm.done();
  }

  /**
   * @param units
   * @throws JavaModelException
   */
  protected final List<ICompilationUnit> getAllProjectCompilationUnits(final IProgressMonitor pm) throws JavaModelException {
    pm.beginTask("Gathering project information...", 1);
    final List<ICompilationUnit> $ = new ArrayList<>();
    for (final IPackageFragmentRoot r : compilationUnit.getJavaProject().getPackageFragmentRoots())
      if (r.getKind() == IPackageFragmentRoot.K_SOURCE)
        for (final IJavaElement e : r.getChildren())
          if (e.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
            $.addAll(Arrays.asList(((IPackageFragment) e).getCompilationUnits()));
    pm.done();
    return $;
  }

  /**
   * Checks a Compilation Unit (outermost ASTNode in the Java Grammar) for
   * spartanization suggestions
   *
   * @param cu
   *          what to check
   * @return a collection of {@link Range} objects each containing a
   *         spartanization opportunity
   */
  public final List<Range> findOpportunities(final CompilationUnit cu) {
    final List<Range> $ = new ArrayList<>();
    cu.accept(fillOpportunities($));
    return $;
  }

  @Override public final Change createChange(@SuppressWarnings("unused") final IProgressMonitor pm)
      throws OperationCanceledException {
    return new CompositeChange(getName(), changes.toArray(new Change[changes.size()]));
  }

  /**
   * @return the selection
   */
  public ITextSelection getSelection() {
    return selection;
  }

  /**
   * @param selection
   *          the selection to set
   */
  public void setSelection(final ITextSelection selection) {
    this.selection = selection;
  }

  /**
   * @return the compilationUnit
   */
  public ICompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * @param compilationUnit
   *          the compilationUnit to set
   */
  public void setCompilationUnit(final ICompilationUnit compilationUnit) {
    this.compilationUnit = compilationUnit;
  }

  protected final boolean inRange(final IMarker m, final ASTNode n) {
    return m == null && isNodeOutsideSelection(n) && isTextSelected() ? false
        : m != null && isNodeOutsideMarker(n, m) ? false : true;
  }

  @Override public String toString() {
    return name;
  }

  /**
   * @return the message to display in the quickfix
   */
  public String getMessage() {
    return message;
  }

  /**
   * @return a quickfix which automatically performs the spartanization
   */
  public IMarkerResolution getFix() {
    return new SpartanizationResolution();
  }

  /**
   * @return a quickfix which opens a refactoring wizard with the spartanization
   */
  public IMarkerResolution getFixWithPreview() {
    return new SpartanizationResolutionWithPreview();
  }

  /**
   * a quickfix which automatically performs the spartanization
   *
   * @author Boris van Sosin <code><boris.van.sosin [at] gmail.com></code>
   * @since 2013/07/01
   */
  public class SpartanizationResolution implements IMarkerResolution {
    @Override public String getLabel() {
      return "Do it! " + Spartanization.this.toString();
    }

    @Override public void run(final IMarker m) {
      try {
        runAsMarkerFix(new NullProgressMonitor(), m);
      } catch (final CoreException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * a quickfix which opens a refactoring wizard with the spartanization
   *
   * @author r Boris van Sosin <code><boris.van.sosin [at] gmail.com></code>
   *         (v2)
   */
  public class SpartanizationResolutionWithPreview implements IMarkerResolution {
    @Override public String getLabel() {
      return Spartanization.this + ": Show me a preview first";
    }

    @Override public void run(final IMarker m) {
      setMarker(m);
      try {
        new RefactoringWizardOpenOperation(new Wizard(Spartanization.this)).run(Display.getCurrent().getActiveShell(),
            "Spartan refactoring: " + Spartanization.this);
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
