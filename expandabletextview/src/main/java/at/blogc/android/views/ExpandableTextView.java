package at.blogc.android.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.reflect.Field;

import at.blogc.android.animator.AnimatorListener;
import at.blogc.expandabletextview.R;

/**
 * Copyright (C) 2016 Cliff Ophalvens (Blogc.at)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ExpandableTextView extends TextView
{
    private static final int DEFAULT_DURATION = 750;
    private static final int MAXMODE_LINES = 1;

    private OnExpandListener onExpandListener;

    private long animationDuration;
    private boolean animating;
    private boolean expanded;
    private int maxLines;
    private int originalHeight;

    public ExpandableTextView(Context context)
    {
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        // read attributes
        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextView, defStyle, 0);
        this.animationDuration = attributes.getInt(R.styleable.ExpandableTextView_animation_duration, DEFAULT_DURATION);
        attributes.recycle();

        // keep the original value of maxLines
        this.maxLines = this.getMaxLines();
    }

    @Override
    public int getMaxLines()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            return super.getMaxLines();
        }

        try
        {
            final Field mMaxMode = TextView.class.getField("mMaxMode");
            mMaxMode.setAccessible(true);
            final Field mMaximum = TextView.class.getField("mMaximum");
            mMaximum.setAccessible(true);

            final int mMaxModeValue = (int) mMaxMode.get(this);
            final int mMaximumValue = (int) mMaximum.get(this);

            return mMaxModeValue == MAXMODE_LINES ? mMaximumValue : -1;
        }
        catch (final Exception e)
        {
           return -1;
        }
    }

    /**
     * Toggle the expanded state of this {@link ExpandableTextView}.
     * @return true if toggled, false otherwise.
     */
    public boolean toggle()
    {
        if (this.expanded)
        {
            return this.collapse();
        }

        return this.expand();
    }

    /**
     * Expand this {@link ExpandableTextView}.
     * @return true if expanded, false otherwise.
     */
    public boolean expand()
    {
        if (!this.expanded && !this.animating && this.maxLines >= 0)
        {
            this.animating = true;

            // notify listener
            if (this.onExpandListener != null)
            {
                this.onExpandListener.onExpand(this);
            }

            // get original height
            this.measure
            (
                MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );

            this.originalHeight = this.getMeasuredHeight();

            // set maxLines to MAX Integer
            this.setMaxLines(Integer.MAX_VALUE);

            // get new height
            this.measure
            (
                MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );

            final int fullHeight = this.getMeasuredHeight();

            final ValueAnimator valueAnimator = ValueAnimator.ofInt(this.originalHeight, fullHeight);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(final ValueAnimator animation)
                {
                    final ViewGroup.LayoutParams layoutParams = ExpandableTextView.this.getLayoutParams();
                    layoutParams.height = (int) animation.getAnimatedValue();
                    ExpandableTextView.this.setLayoutParams(layoutParams);
                }
            });
            valueAnimator.addListener(new AnimatorListener()
            {
                @Override
                public void onAnimationEnd(final Animator animation)
                {
                    ExpandableTextView.this.expanded = true;
                    ExpandableTextView.this.animating = false;
                }
            });

            // start the animation
            valueAnimator
                .setDuration(this.animationDuration)
                .start();

            return true;
        }

        return false;
    }

    /**
     * Collapse this {@link TextView}.
     * @return true if collapsed, false otherwise.
     */
    public boolean collapse()
    {
        if (this.expanded && !this.animating && this.maxLines >= 0)
        {
            this.animating = true;

            // notify listener
            if (this.onExpandListener != null)
            {
                this.onExpandListener.onCollapse(this);
            }

            // get new height
            final int fullHeight = this.getMeasuredHeight();

            final ValueAnimator valueAnimator = ValueAnimator.ofInt(fullHeight, this.originalHeight);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(final ValueAnimator animation)
                {
                    final ViewGroup.LayoutParams layoutParams = ExpandableTextView.this.getLayoutParams();
                    layoutParams.height = (int) animation.getAnimatedValue();
                    ExpandableTextView.this.setLayoutParams(layoutParams);
                }
            });
            valueAnimator.addListener(new AnimatorListener()
            {
                @Override
                public void onAnimationEnd(final Animator animation)
                {
                    // set maxLines to original value
                    ExpandableTextView.this.setMaxLines(ExpandableTextView.this.maxLines);

                    ExpandableTextView.this.expanded = false;
                    ExpandableTextView.this.animating = false;
                }
            });

            // start the animation
            valueAnimator
                .setDuration(this.animationDuration)
                .start();

            return true;
        }

        return false;
    }

    /**
     * Sets the duration of the expand / collapse animation.
     * @param animationDuration duration in milliseconds.
     */
    public void setAnimationDuration(final long animationDuration)
    {
        this.animationDuration = animationDuration;
    }

    /**
     * Sets a listener which receives updates about this {@link ExpandableTextView}.
     * @param onExpandListener the listener.
     */
    public void setOnExpandListener(final OnExpandListener onExpandListener)
    {
        this.onExpandListener = onExpandListener;
    }

    /**
     * Returns the {@link OnExpandListener}.
     * @return the listener.
     */
    public OnExpandListener getOnExpandListener()
    {
        return onExpandListener;
    }

    /**
     * Is this {@link ExpandableTextView} expanded or not?
     * @return true if expanded, false if collapsed.
     */
    public boolean isExpanded()
    {
        return this.expanded;
    }

    public interface OnExpandListener
    {
        void onExpand(ExpandableTextView view);
        void onCollapse(ExpandableTextView view);
    }
}