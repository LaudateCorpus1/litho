/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static com.facebook.litho.sections.Change.DELETE;
import static com.facebook.litho.sections.Change.DELETE_RANGE;
import static com.facebook.litho.sections.Change.INSERT;
import static com.facebook.litho.sections.Change.INSERT_RANGE;
import static com.facebook.litho.sections.Change.MOVE;
import static com.facebook.litho.sections.Change.UPDATE;
import static com.facebook.litho.sections.Change.UPDATE_RANGE;

import android.support.annotation.VisibleForTesting;
import com.facebook.litho.sections.SectionTree.Target;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.widget.RenderInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * A ChangeSet represent a list of Change that has to be applied to a
 * {@link Target} as the result of an update of a
 * {@link Section}. A ChangeSet is provided in the
 * {@link OnDiff} of a
 * {@link DiffSectionSpec} to allow the ChangeSetSpec to
 * define its changes based on old/new props and state.
 */
public final class ChangeSet {

  private final List<Change> mChanges;
  private int mFinalCount;

  private ChangeSet() {
    mChanges = new ArrayList<>();
    mFinalCount = 0;
  }

  /**
   * @return the {@link Change} at index.
   */
  public Change getChangeAt(int index) {
    return mChanges.get(index);
  }

  /**
   * @return the number of {@link Change}s in this ChangeSet.
   */
  public int getChangeCount() {
    return mChanges.size();
  }

  /**
   * Add a new Change to this ChangeSet. This is what a {@link DiffSectionSpec} would call in its
   * {@link OnDiff} method to append a {@link Change}.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void addChange(Change change) {
    int changeDelta = 0;
    switch (change.getType()) {
      case INSERT:
        changeDelta = 1;
        break;
      case INSERT_RANGE:
        changeDelta = change.getCount();
        break;
      case DELETE:
        changeDelta = -1;
        break;
      case DELETE_RANGE:
        changeDelta = -change.getCount();
        break;
      case UPDATE:
      case UPDATE_RANGE:
      case MOVE:
      default:
        break;
    }

    mFinalCount += changeDelta;
    mChanges.add(change);
  }

  public void insert(int index, RenderInfo renderInfo) {
    addChange(Change.insert(index, renderInfo));
  }

  public void insertRange(int index, int count, List<RenderInfo> renderInfos) {
    addChange(Change.insertRange(index, count, renderInfos));
  }

  public void update(int index, RenderInfo renderInfo) {
    addChange(Change.update(index, renderInfo));
  }

  public void updateRange(int index, int count, List<RenderInfo> renderInfos) {
    addChange(Change.updateRange(index, count, renderInfos));
  }

  public void delete(int index) {
    addChange(Change.remove(index));
  }

  public void deleteRange(int index, int count) {
    addChange(Change.removeRange(index, count));
  }

  public void move(int fromIndex, int toIndex) {
    addChange(Change.move(fromIndex, toIndex));
  }

  /**
   * @return the total number of items in the {@link Target}
   * after this ChangeSet will be applied.
   */
  int getCount() {
    return mFinalCount;
  }

  /** @return an empty ChangeSet. */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static ChangeSet acquireChangeSet() {
    return acquireChangeSet(0);
  }

  /**
   * @return an empty ChangeSet starting from count startCount.
   */
  static ChangeSet acquireChangeSet(int startCount) {
    final ChangeSet changeSet = acquire();
    changeSet.mFinalCount = startCount;

    return changeSet;
  }

  /**
   * Used internally by the framework to merge all the ChangeSet generated by all the leaf {@link
   * Section}. The merged ChangeSet will be passed to the {@link Target}.
   */
  static ChangeSet merge(ChangeSet first, ChangeSet second) {
    final ChangeSet mergedChangeSet = acquireChangeSet();
    final int firstCount = first != null ? first.mFinalCount : 0;
    final int secondCount = second != null ? second.mFinalCount : 0;

    List<Change> mergedChanged =mergedChangeSet.mChanges;

    if (first != null) {
      for (Change change : first.mChanges) {
        mergedChanged.add(Change.copy(change));
      }
    }

    if (second != null) {
      for (Change change : second.mChanges) {
        mergedChanged.add(Change.offset(change, firstCount));
      }
    }

    mergedChangeSet.mFinalCount = firstCount + secondCount;

    return mergedChangeSet;
  }

  //TODO implement pools t11953296
  private static ChangeSet acquire() {
    return new ChangeSet();
  }

  //TODO implement pools t11953296
  void release() {
    for (Change change : mChanges) {
      change.release();
    }

    mChanges.clear();
    mFinalCount = 0;
  }
}
