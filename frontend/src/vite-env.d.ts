// Allows TypeScript to import CSS Modules as typed objects
declare module '*.module.css' {
  const classes: { readonly [key: string]: string };
  export default classes;
}

// Plain CSS imports (e.g. global stylesheets)
declare module '*.css' {
  const content: string;
  export default content;
}
