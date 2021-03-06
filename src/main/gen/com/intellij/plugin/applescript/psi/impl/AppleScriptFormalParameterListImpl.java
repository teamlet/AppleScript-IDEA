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

public class AppleScriptFormalParameterListImpl extends AppleScriptPsiElementImpl implements AppleScriptFormalParameterList {

  public AppleScriptFormalParameterListImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull AppleScriptVisitor visitor) {
    visitor.visitFormalParameterList(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AppleScriptVisitor) accept((AppleScriptVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<AppleScriptExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AppleScriptExpression.class);
  }

  @Override
  @NotNull
  public List<AppleScriptListFormalParameter> getListFormalParameterList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AppleScriptListFormalParameter.class);
  }

  @Override
  @NotNull
  public List<AppleScriptRecordFormalParameter> getRecordFormalParameterList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AppleScriptRecordFormalParameter.class);
  }

  @Override
  @NotNull
  public List<AppleScriptSimpleFormalParameter> getSimpleFormalParameterList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, AppleScriptSimpleFormalParameter.class);
  }

  @NotNull
  public List<AppleScriptComponent> getFormalParameters() {
    return AppleScriptPsiImplUtil.getFormalParameters(this);
  }

}
