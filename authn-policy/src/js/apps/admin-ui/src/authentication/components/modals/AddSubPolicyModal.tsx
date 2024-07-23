import {Button, ButtonVariant, Form, Modal, ModalVariant,} from "@patternfly/react-core";
import {FormProvider, useForm} from "react-hook-form";
import {useTranslation} from "react-i18next";
import {TextControl} from "@keycloak/keycloak-ui-shared";

type AddSubPolicyProps = {
    name: string;
    onConfirm: (flow: Policy) => void;
    onCancel: () => void;
};

export type Policy = {
    name: string;
    description: string;
    providerId: string;
};

export const AddSubPolicyModal = ({
                                    name,
                                    onConfirm,
                                    onCancel,
                                }: AddSubPolicyProps) => {
    const { t } = useTranslation();
    const form = useForm<Policy>();

    return (
        <Modal
            variant={ModalVariant.medium}
            title={t("addPolicy", {name})}
            onClose={onCancel}
            actions={[
                <Button
                    key="add"
                    data-testid="modal-add"
                    type="submit"
                    form="sub-flow-form"
                >
                    {t("add")}
                </Button>,
                <Button
                    key="cancel"
                    data-testid="cancel"
                    variant={ButtonVariant.link}
                    onClick={onCancel}
                >
                    {t("cancel")}
                </Button>,
            ]}
            isOpen
        >
            <Form
                id="sub-flow-form"
                onSubmit={form.handleSubmit(onConfirm)}
                isHorizontal
            >
                <FormProvider {...form}>
                    <TextControl
                        name="name"
                        label={t("name")}
                        labelIcon={t("clientIdHelp")}
                        rules={{ required: { value: true, message: t("required") } }}
                    />
                    <TextControl
                        name="description"
                        label={t("description")}
                        labelIcon={t("flowNameDescriptionHelp")}
                    />
                </FormProvider>
            </Form>
        </Modal>
    );
};
