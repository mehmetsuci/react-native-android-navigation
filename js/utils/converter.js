import Intent from "../Intent";

export default class Converter {

    static getIntentReader(getter) {
        return (source) => {
            getter(this.readIntent(source));
        }
    }

    static readIntent(source) {
        const result = new Intent();
        result.className = source.className;
        result.packageName = source.packageName;
        result.action = source.action;
        result.extras = source.extras;
        result.categories = source.categories;
        result.flags = [source.flags];
        return result;
    }

    static writeIntent(source) {
        return {
            className: source.className,
            packageName: source.packageName,
            action: source.action,
            customServiceEventName: source.customServiceEventName,
            extras: source.extras,
            categories: source.categories,
            flags: source.flags,
        }
    }

}