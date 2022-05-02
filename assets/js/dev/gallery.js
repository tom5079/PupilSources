function get_gallery(galleryid) {
    const url = `https://ltn.hitomi.la/galleries/${galleryid}.js`;

    return new Promise((resolve, reject) => {
        $.getScript(url, (data) => {
            resolve({
                related: galleryinfo.related,
                langList: galleryinfo.languages?.reduce((dict, lang) => ({...dict, [lang.name]: lang.galleryid}), {}) ?? [],
                cover: url_from_url_from_hash(galleryid, galleryinfo.files[0], 'webpbigtn', 'webp', 'tn'),
                title: galleryinfo.title,
                artists: galleryinfo.artists?.map(artist => artist.artist) ?? [],
                groups: galleryinfo.groups?.map(group => group.group) ?? [],
                type: galleryinfo.type,
                language: galleryinfo.language,
                series: galleryinfo.parodys?.map(parody => parody.parody) ?? [],
                characters: galleryinfo.characters?.map(character => character.character) ?? [],
                tags: galleryinfo.tags?.map(tag => `${tag.female ? "female:" : ""}${tag.male ? "male:" : ""}${tag.tag}`) ?? [],
                thumbnails: galleryinfo.files.map(file => url_from_url_from_hash(galleryid, file, 'webpsmalltn', 'webp', 'tn'))
            });
        });
    });
}