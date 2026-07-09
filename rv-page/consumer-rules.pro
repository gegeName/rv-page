# Public custom view. Keep the class name for XML inflation or external
# string-based references, but still allow removal when it is unused.
-keep,allowshrinking,allowoptimization class com.chat.rv_page.NestedCarouselRecyclerView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
