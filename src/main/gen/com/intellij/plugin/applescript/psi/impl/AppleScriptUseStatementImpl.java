// This is a generated file. Not intended for manual editing.
package com.intellij.plugin.applescript.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugin.applescript.psi.AppleScriptTypes.*;
import com.intellij.plugin.applescript.psi.*;

public class AppleScriptUseStatementImpl extends AppleScriptPsiElementImpl implements AppleScriptUseStatement {

  public AppleScriptUseStatementImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AppleScriptVisitor visitor) {
    visitor.visitUseStatement(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AppleScriptVisitor) accept((AppleScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public AppleScriptDirectParameterDeclaration getDirectParameterDeclaration() {
    return findChildByClass(AppleScriptDirectParameterDeclaration.class);
  }

  @Override
  @NotNull
  public List<AppleScriptExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AppleScriptExpression.class);
  }

  @Nullable
  public String getApplicationName() {
    return AppleScriptPsiImplUtil.getApplicationName(this);
  }

  public boolean useStandardAdditions() {
    return AppleScriptPsiImplUtil.useStandardAdditions(this);
  }

  public boolean withImporting() {
    return AppleScriptPsiImplUtil.withImporting(this);
  }

}
